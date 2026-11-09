#Requires -Version 7.0
param(
    [string]$MySqlBase = 'C:\Program Files\MySQL\MySQL Server 8.0'
)
$ErrorActionPreference = 'Stop'
$projectDir = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$serverExe = Join-Path $MySqlBase 'bin\mysqld.exe'
$clientExe = Join-Path $MySqlBase 'bin\mysql.exe'
$adminExe = Join-Path $MySqlBase 'bin\mysqladmin.exe'
foreach ($exe in @($serverExe, $clientExe, $adminExe)) {
    if (-not (Test-Path -LiteralPath $exe)) { throw "No se encuentra $exe" }
}
$runId = [Guid]::NewGuid().ToString('N')
$runDir = Join-Path $projectDir "target\dentia-mysql-checks\$runId"
$dataDir = Join-Path $runDir 'data'
$null = New-Item -ItemType Directory -Path $runDir
# Puerto efímero libre. Una carrera se detecta mediante @@datadir antes de cualquier escritura.
$listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
$listener.Start()
$testPort = $listener.LocalEndpoint.Port
$listener.Stop()
$schema = "dentia_test_$runId"
$serverProcess = $null
$ownServerVerified = $false
$savedUrl = $env:DENTIA_TEST_MYSQL_URL
$savedData = $env:DENTIA_TEST_MYSQL_DATADIR
Push-Location $projectDir
try {
    Write-Host "Instancia MySQL aislada: $runDir (puerto $testPort)"
    $init = Start-Process -FilePath $serverExe -ArgumentList @(
        '--no-defaults', '--initialize-insecure', "--basedir=`"$MySqlBase`"", "--datadir=`"$dataDir`"",
        '--innodb-redo-log-capacity=64M', '--console'
    ) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runDir 'init.out.log') -RedirectStandardError (Join-Path $runDir 'init.err.log')
    $null = $init.Handle
    if (-not $init.WaitForExit(60000)) { $init.Kill(); throw 'La inicialización de MySQL excedió el tiempo permitido.' }
    $init.WaitForExit()
    if ($init.ExitCode -ne 0) { throw "MySQL no pudo inicializarse. Revisa $runDir\init.err.log" }
    $serverProcess = Start-Process -FilePath $serverExe -ArgumentList @(
        '--no-defaults', "--basedir=`"$MySqlBase`"", "--datadir=`"$dataDir`"",
        "--port=$testPort", '--bind-address=127.0.0.1', '--mysqlx=OFF', '--skip-log-bin',
        '--innodb-buffer-pool-size=64M', '--innodb-redo-log-capacity=64M', '--console'
    ) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runDir 'server.out.log') -RedirectStandardError (Join-Path $runDir 'server.err.log')
    $clientArgs = @('--no-defaults', '--protocol=TCP', '--host=127.0.0.1', "--port=$testPort", '--user=root', '--skip-password', '--connect-timeout=2', '--batch', '--raw', '--skip-column-names')
    $ready = $false
    for ($attempt = 0; $attempt -lt 50; $attempt++) {
        if ($serverProcess.HasExited) { throw "La instancia temporal se detuvo. Revisa $runDir\server.err.log" }
        $remoteData = & $clientExe @clientArgs '--execute=SELECT @@datadir' 2>$null
        if ($LASTEXITCODE -eq 0) {
            $actual = [IO.Path]::GetFullPath(($remoteData | Select-Object -Last 1).Trim()).TrimEnd('\','/')
            $expected = [IO.Path]::GetFullPath($dataDir).TrimEnd('\','/')
            if ($actual -ne $expected) { throw 'El puerto corresponde a otra instancia. No se modificó ninguna base.' }
            $ownServerVerified = $true
            $ready = $true
            break
        }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw 'MySQL temporal no estuvo disponible a tiempo.' }
    & $clientExe @clientArgs "--execute=CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el esquema aislado.' }
    $env:DENTIA_TEST_MYSQL_URL = "jdbc:mysql://127.0.0.1:$testPort/$schema" + '?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
    $env:DENTIA_TEST_MYSQL_DATADIR = $dataDir
    & mvn -B -ntp -Pintegration '-Ddentia.build.directory=target/dentia-verification' '-Dtest=ClinicaMysqlIntegrationTest' test
    if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas MySQL; consulta los informes de Surefire.' }
    Write-Host 'Pruebas MySQL terminadas correctamente.'
} finally {
    $env:DENTIA_TEST_MYSQL_URL = $savedUrl
    $env:DENTIA_TEST_MYSQL_DATADIR = $savedData
    # MySQL en Windows puede mantener un proceso hijo aunque el lanzador ya haya salido.
    # Se identifica exclusivamente por ejecutable y datadir UUID de esta ejecución.
    $ownedProcesses = @(Get-CimInstance Win32_Process -Filter "Name='mysqld.exe'" | Where-Object {
        $_.ExecutablePath -eq $serverExe -and $_.CommandLine.Contains($dataDir)
    })
    if ($ownedProcesses.Count -gt 0) {
        if ($ownServerVerified) {
            & $adminExe '--no-defaults' '--protocol=TCP' '--host=127.0.0.1' "--port=$testPort" '--user=root' '--skip-password' '--connect-timeout=2' 'shutdown' 2>$null
        }
        foreach ($owned in @(Get-CimInstance Win32_Process -Filter "Name='mysqld.exe'" | Where-Object {
            $_.ExecutablePath -eq $serverExe -and $_.CommandLine.Contains($dataDir)
        })) {
            $remainingProcess = Get-Process -Id $owned.ProcessId -ErrorAction SilentlyContinue
            if ($remainingProcess -and -not $remainingProcess.WaitForExit(10000)) {
                $remainingProcess.Kill()
                $remainingProcess.WaitForExit()
            }
        }
    }
    Pop-Location
    Write-Host "Instancia temporal detenida. Datos sintéticos y logs conservados en $runDir"
}
