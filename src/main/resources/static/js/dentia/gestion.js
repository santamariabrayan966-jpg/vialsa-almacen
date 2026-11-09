(() => {
    'use strict';
    const root = document.getElementById('gestion');
    if (!root) return;
    const module = root.dataset.modulo;
    const base = '/interno/gestion/';
    const $ = selector => document.querySelector(selector);
    const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    const money = value => new Intl.NumberFormat('es-PE', {style:'currency',currency:'PEN'}).format(Number(value || 0));
    const today = () => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; };
    const date = value => value ? new Intl.DateTimeFormat('es-PE', {day:'2-digit',month:'short',year:'numeric',...(String(value).includes('T')?{hour:'2-digit',minute:'2-digit'}:{})}).format(new Date(String(value).length===10?value+'T12:00:00':value)) : '—';
    const labels = {BORRADOR:'Borrador',APROBADO:'Aprobado',RECHAZADO:'Rechazado',ANULADO:'Anulado',REGISTRADO:'Registrado',PENDIENTE:'Pendiente',COMPLETADO:'Completado',CANCELADO:'Cancelado',ACTIVO:'Activo',ARCHIVADO:'Archivado',EFECTIVO:'Efectivo',TARJETA:'Tarjeta',TRANSFERENCIA:'Transferencia',YAPE:'Yape',PLIN:'Plin',LLAMADA:'Llamada',WHATSAPP:'WhatsApp',CORREO:'Correo',PRESENCIAL:'Presencial',CONSENTIMIENTO:'Consentimiento',INFORME:'Informe',RECETA:'Receta',COMPROBANTE:'Comprobante',OTRO:'Otro'};
    const label = value => labels[value] || value || '—';
    const pill = value => `<span class="gestion-pill ${escape(String(value).toLowerCase())}">${escape(label(value))}</span>`;
    const config = {
        seguimiento:{title:'Seguimiento',description:'Organiza los próximos contactos y conserva el resultado de cada seguimiento.',create:'Programar seguimiento',list:'Seguimientos del paciente',icon:'arrow-repeat',states:['PENDIENTE','COMPLETADO','CANCELADO'],empty:'Sin seguimientos en esta vista',hint:'Programa un contacto y vincúlalo, si corresponde, a una cita o presupuesto.'},
        documentos:{title:'Documentos',description:'Un archivo organizado por paciente, con descarga protegida y trazabilidad.',create:'Adjuntar documento',list:'Archivo de documentos',icon:'file-earmark-medical',states:['ACTIVO','ARCHIVADO'],empty:'Sin documentos en esta vista',hint:'Adjunta archivos PDF, JPG o PNG de hasta 1 MB. Solo el personal autorizado podrá descargarlos.'},
        presupuestos:{title:'Presupuestos',description:'Del borrador a la aprobación, con conceptos detallados y saldos conectados.',create:'Nuevo presupuesto',list:'Presupuestos registrados',icon:'receipt',states:['BORRADOR','APROBADO','RECHAZADO','ANULADO'],empty:'Sin presupuestos en esta vista',hint:'Crea un presupuesto con sus conceptos. Al aprobarlo podrás registrar los abonos.'},
        pagos:{title:'Pagos',description:'Registra los cobros y consulta el saldo real de cada presupuesto aprobado.',create:'Registrar abono',list:'Movimientos de cobro',icon:'credit-card',states:['REGISTRADO','ANULADO'],empty:'Sin cobros en esta vista',hint:'Los abonos se registran sobre presupuestos aprobados. No se realizan cargos bancarios desde aquí.'},
        reportes:{title:'Reportes',description:'Indicadores reales de cobranza y cartera para tomar decisiones.',create:'Exportar cobros CSV',list:'Resumen del período',icon:'bar-chart',states:[]}
    }[module];
    let page=0, pages=0, loadSequence=0, optionSequence=0, current=null, editorAction=null, busy=false;
    let records=[];
    const filters=$('#filtros');
    const field = name => filters.elements.namedItem(name);
    const allowed = new Set([...document.querySelectorAll('[data-seccion]')].map(a=>a.dataset.seccion));
    const params = new URLSearchParams(location.search);
    $('#titulo').textContent=config.title; document.title=config.title+' | DentIA';
    $('#subtitulo').textContent=config.description; $('#crear span').textContent=config.create;
    $('#crear i').className='bi bi-'+(module==='reportes'?'download':'plus-lg');
    $('#lista-titulo').textContent=config.list;
    field('estado').innerHTML='<option value="">Todos los estados</option>'+config.states.map(s=>`<option value="${s}">${label(s)}</option>`).join('');
    for (const name of ['q','desde','hasta','estado']) if(params.has(name))field(name).value=params.get(name);
    if(module==='reportes') {
        $('#estado-filtro').hidden=true;
        if(!field('desde').value)field('desde').value=today().slice(0,8)+'01';
        if(!field('hasta').value)field('hasta').value=today();
        $('#filtro-ayuda').textContent='Cobros por fecha de pago y presupuestos por fecha de creación. Los saldos se calculan al momento de consultar.';
    }
    document.querySelector(`[data-seccion="${module}"]`)?.classList.add('active');
    document.querySelector(`[data-seccion="${module}"]`)?.setAttribute('aria-current','page');
    document.querySelectorAll('.dentia-sidebar a').forEach(a=>{
        if(a.getAttribute('href')==='/interno/modulos/'+module||a.getAttribute('href')===base+module){a.classList.add('active');a.setAttribute('aria-current','page');}
    });

    async function api(path, options={}) {
        const headers=new Headers(options.headers||{});
        headers.set('Accept','application/json');
        if(options.body && !(options.body instanceof FormData)){headers.set('Content-Type','application/json');options.body=JSON.stringify(options.body);}
        if(options.method && options.method!=='GET')headers.set($('meta[name="csrf-header"]').content,$('meta[name="csrf-token"]').content);
        const response=await fetch(base+path,{...options,headers,credentials:'same-origin',cache:'no-store'});
        if(response.redirected||response.status===401)throw new Error('La sesión venció. Inicia sesión y vuelve a intentarlo.');
        const isJson=response.headers.get('content-type')?.includes('json');
        const data=isJson?await response.json():null;
        if(!response.ok)throw new Error(data?.error|| (response.status===403?'No tienes permiso para esta acción o la sesión venció. Actualiza la página.':response.status===413?'El archivo supera el límite de 1 MB.':'No se pudo completar la operación. Inténtalo nuevamente.'));
        if(!data)throw new Error('Respuesta inesperada. Actualiza la página e inicia sesión si es necesario.');
        return data;
    }
    function notice(message,error=false){const el=$('#aviso');el.textContent=message;el.className=error?'error':'';el.hidden=false;}
    function query(){const q=new URLSearchParams(new FormData(filters));for(const [k,v]of [...q])if(!v)q.delete(k);return q;}
    function syncLinks(){const id=field('paciente').value;document.querySelectorAll('[data-seccion]').forEach(a=>a.href=base+a.dataset.seccion+(id?'?paciente='+encodeURIComponent(id):''));}
    async function filterPatients(selected,term='') {
        const result=await api(module+'/opciones?'+new URLSearchParams({...(selected?{paciente:selected}:{}),q:term}));
        field('paciente').innerHTML='<option value="">Todos los pacientes</option>'+result.pacientes.map(p=>`<option value="${p.id}">${escape(p.nombre)}${p.dni?' · '+escape(p.dni):''}</option>`).join('');
        if(selected)field('paciente').value=selected;syncLinks();
    }
    function metric(title,value,icon,note=''){return `<article class="gestion-metric"><div class="gestion-metric-icon"><i class="bi bi-${icon}"></i></div><div><small>${escape(title)}</small><strong>${escape(value)}</strong>${note?`<p>${escape(note)}</p>`:''}</div></article>`;}
    function renderMetrics(data){const s=data.resumen||{};const metrics={
        presupuestos:()=>metric('Presupuestos',s.registros,'receipt','En la selección actual')+metric('Importe aprobado',money(s.aprobado),'check2-circle','Solo presupuestos aprobados')+metric('Saldo por cobrar',money(s.pendiente),'wallet2','Descontando abonos vigentes'),
        pagos:()=>metric('Operaciones',s.registros,'credit-card')+metric('Cobrado vigente',money(s.cobrado),'cash-coin','Excluye pagos anulados')+metric('Importe anulado',money(s.anulado),'arrow-counterclockwise','Conservado para auditoría'),
        seguimiento:()=>metric('Seguimientos',s.registros,'arrow-repeat')+metric('Pendientes',s.pendientes,'clock')+metric('Vencidos',s.vencidos,'exclamation-circle','Pendientes con fecha pasada'),
        documentos:()=>metric('Documentos',s.registros,'files')+metric('Activos',s.activos,'file-earmark-check')+metric('Tamaño total',(Number(s.bytes||0)/1048576).toFixed(2)+' MB','hdd','Archivos de la selección'),
        reportes:()=>metric('Cobros vigentes',money(data.cobros.cobrado),'cash-coin','Por fecha de cobro')+metric('Saldo de la cartera',money(data.presupuestos.pendiente),'wallet2','Presupuestos creados en el período')};
        $('#indicadores').innerHTML=metrics[module]();
    }
    const patientCell=r=>`<strong>${escape(r.paciente)}</strong><small>${r.dni?'DNI '+escape(r.dni):'Paciente #'+r.pacienteId}</small>`;
    const detailButton=r=>`<button class="gestion-open" data-detail="${r.id}" type="button" aria-label="Ver detalle del registro ${r.id}">Ver detalle <i class="bi bi-arrow-up-right"></i></button>`;
    function renderMobileCardsList(mod,recs){
        if(!recs||!recs.length)return '';
        return recs.map(r=>{
            if(mod==='pagos'){
                return `<div class="gestion-m-card">
                    <div class="gestion-m-card-top">
                        <div class="gestion-m-patient">
                            <strong>${escape(r.paciente)}</strong>
                            <small>${r.dni?'DNI '+escape(r.dni):'Paciente #'+r.pacienteId}</small>
                        </div>
                        <div>${pill(r.estado)}</div>
                    </div>
                    <div class="gestion-m-card-body">
                        <div>
                            <span class="gestion-m-op">Cobro #${r.id}</span>
                            <small class="d-block text-secondary">Presupuesto #${r.presupuesto_id} · ${escape(label(r.metodo))}</small>
                        </div>
                        <div class="gestion-m-amount">
                            <strong>${money(r.monto)}</strong>
                            <small>${date(r.fecha)}</small>
                        </div>
                    </div>
                    <div class="gestion-m-card-foot">
                        <small class="text-secondary">${escape(r.referencia||'Pago en caja')}</small>
                        ${detailButton(r)}
                    </div>
                </div>`;
            }
            if(mod==='presupuestos'){
                return `<div class="gestion-m-card">
                    <div class="gestion-m-card-top">
                        <div class="gestion-m-patient">
                            <strong>${escape(r.paciente)}</strong>
                            <small>${r.dni?'DNI '+escape(r.dni):'Paciente #'+r.pacienteId}</small>
                        </div>
                        <div>${pill(r.estado)}</div>
                    </div>
                    <div class="gestion-m-card-body">
                        <div>
                            <span class="gestion-m-op">#${r.id} · ${escape(r.titulo)}</span>
                            <small class="d-block text-secondary">Vigencia: ${date(r.vence)}</small>
                        </div>
                        <div class="gestion-m-amount">
                            <strong>${money(r.total)}</strong>
                            <small>Saldo: ${money(r.saldo)}</small>
                        </div>
                    </div>
                    <div class="gestion-m-card-foot">
                        <small class="text-secondary">Abonado: ${money(r.pagado)}</small>
                        ${detailButton(r)}
                    </div>
                </div>`;
            }
            return `<div class="gestion-m-card">
                <div class="gestion-m-card-top">
                    <div class="gestion-m-patient">
                        <strong>${escape(r.paciente||r.titulo||'Registro')}</strong>
                        <small>${r.dni?'DNI '+escape(r.dni):''}</small>
                    </div>
                    <div>${pill(r.estado)}</div>
                </div>
                <div class="gestion-m-card-body">
                    <div>
                        <span class="gestion-m-op">${escape(r.titulo||('#'+r.id))}</span>
                        <small class="d-block text-secondary">${r.fecha?date(r.fecha):(r.creado_en?date(r.creado_en):'')}</small>
                    </div>
                    ${r.total?`<div class="gestion-m-amount"><strong>${money(r.total)}</strong></div>`:''}
                </div>
                <div class="gestion-m-card-foot">
                    <small class="text-secondary">${r.metodo?escape(label(r.metodo)):''}</small>
                    ${detailButton(r)}
                </div>
            </div>`;
        }).join('');
    }
    function table(headers,rows){
        return `<div class="gestion-table-wrap d-none d-lg-block"><table class="gestion-table"><thead><tr>${headers.map(h=>`<th scope="col">${h}</th>`).join('')}</tr></thead><tbody>${rows.map(cells=>`<tr>${cells.map(c=>`<td>${c}</td>`).join('')}</tr>`).join('')}</tbody></table></div>
        <div class="gestion-cards-mobile d-lg-none">${renderMobileCardsList(module,records)}</div>`;
    }
    function renderRows(data){
        records=data.filas;pages=Number(data.paginas);
        $('#conteo').textContent=`${data.total} registro${Number(data.total)===1?'':'s'} · 20 por página`;
        $('#pagina').textContent=pages?`Página ${page+1} de ${pages}`:'Sin resultados';
        $('#anterior').disabled=page===0;$('#siguiente').disabled=page+1>=pages;$('#paginacion').hidden=false;
        if(!records.length){$('#contenido').innerHTML=`<div class="gestion-empty"><i class="bi bi-${config.icon}"></i><strong>${config.empty}</strong><p>${config.hint} Si ya hay registros, revisa los filtros.</p></div>`;return;}
        const content={
            presupuestos:()=>table(['Paciente','Presupuesto','Total','Saldo','Estado','Acción'],records.map(r=>[patientCell(r),`<strong>#${r.id} · ${escape(r.titulo)}</strong><small>Vigencia: ${date(r.vence)}</small>`,`<span class="gestion-money">${money(r.total)}</span>`,`<strong class="gestion-money">${money(r.saldo)}</strong><small>Abonado: ${money(r.pagado)}</small>`,pill(r.estado),detailButton(r)])),
            pagos:()=>table(['Paciente','Operación','Fecha','Importe','Estado','Acción'],records.map(r=>[patientCell(r),`<strong>Cobro #${r.id}</strong><small>Presupuesto #${r.presupuesto_id} · ${escape(label(r.metodo))}</small>`,date(r.fecha),`<strong class="gestion-money">${money(r.monto)}</strong><small>${escape(r.referencia||'Sin referencia')}</small>`,pill(r.estado),detailButton(r)])),
            seguimiento:()=>table(['Paciente','Seguimiento','Programado','Canal','Estado','Acción'],records.map(r=>[patientCell(r),`<strong>${escape(r.titulo)}</strong><small>${r.presupuesto_id?'Presupuesto #'+r.presupuesto_id:r.cita_id?'Cita #'+r.cita_id:'Contacto del paciente'}</small>`,`<span class="${r.estado==='PENDIENTE'&&new Date(r.fecha)<new Date()?'gestion-overdue':''}">${date(r.fecha)}</span>`,escape(label(r.canal)),pill(r.estado),detailButton(r)])),
            documentos:()=>table(['Paciente','Documento','Tipo','Adjuntado','Estado','Acción'],records.map(r=>[patientCell(r),`<strong>${escape(r.titulo)}</strong><small>${escape(r.nombre_archivo)} · ${(r.tamano/1024).toFixed(0)} KB</small>`,escape(label(r.tipo)),date(r.creado_en),pill(r.estado),detailButton(r)]))};
        $('#contenido').innerHTML=content[module]();
    }
    function bars(rows,name){const max=Math.max(1,...rows.map(r=>Number(r.total)));return rows.length?rows.map(r=>`<div class="gestion-bar-row"><div class="gestion-bar-label"><span>${escape(name(r))}</span><strong>${money(r.total)}</strong></div><div class="gestion-bar-track"><div class="gestion-bar-fill" style="width:${Math.max(1,Number(r.total)/max*100)}%"></div></div></div>`).join(''):'<p class="text-secondary small">No hay cobros vigentes en el período.</p>';}
    function renderReport(data){
        $('#conteo').textContent=`${date(field('desde').value)} — ${date(field('hasta').value)}`;$('#paginacion').hidden=true;
        $('#contenido').innerHTML=`<div class="gestion-report-grid"><section><h3>Cobros por método</h3>${bars(data.metodos,r=>label(r.metodo))}</section><section><h3>Evolución diaria de cobros</h3>${bars(data.dias,r=>date(r.fecha))}</section></div><div class="gestion-results-head"><h2>Cartera por paciente</h2><span class="small text-secondary">Hasta 20 pacientes con mayor saldo</span></div>${data.cartera.length?table(['Paciente','Aprobado','Abonado','Saldo'],data.cartera.map(r=>[allowed.has('presupuestos')?`<a href="${base}presupuestos?paciente=${r.pacienteId}">${escape(r.paciente)}</a>`:escape(r.paciente),money(r.total),money(r.pagado),`<strong>${money(r.saldo)}</strong>`])):'<div class="gestion-empty">No hay presupuestos aprobados en este período.</div>'}<p class="gestion-report-note mt-3">Cartera de presupuestos creados en las fechas seleccionadas; incluye todos sus abonos vigentes, incluso fuera del período. Cobros anulados: ${money(data.cobros.anulado)}. No es un reporte tributario.</p>`;
    }
    async function load(){
        const sequence=++loadSequence;$('#contenido').setAttribute('aria-busy','true');$('#actualizar').disabled=true;
        const q=query();q.set('pagina',String(page));history.replaceState(null,'',location.pathname+'?'+query().toString());syncLinks();
        try{const data=await api(module+'/datos?'+q);if(sequence!==loadSequence)return;renderMetrics(data);module==='reportes'?renderReport(data):renderRows(data);$('#crear').disabled=false;}
        catch(error){if(sequence===loadSequence){notice(error.message,true);$('#contenido').innerHTML='<div class="gestion-empty"><strong>No se pudo cargar la información</strong><p>Revisa el aviso e intenta actualizar. No se ha reemplazado ningún registro.</p></div>';$('#paginacion').hidden=true;}}
        finally{if(sequence===loadSequence){$('#contenido').setAttribute('aria-busy','false');$('#actualizar').disabled=false;}}
    }
    const input=(name,title,type,value='',attrs='')=>`<label>${title}<input class="form-control" name="${name}" type="${type}" value="${escape(value)}" ${attrs}></label>`;
    const select=(name,title,values,value='',attrs='')=>`<label>${title}<select class="form-select" name="${name}" ${attrs}>${values.map(([id,text])=>`<option value="${escape(id)}" ${String(id)===String(value)?'selected':''}>${escape(text)}</option>`).join('')}</select></label>`;
    const textarea=(name,title,value='',attrs='maxlength="2000"')=>`<label class="full">${title}<textarea class="form-control" name="${name}" rows="3" ${attrs}>${escape(value)}</textarea></label>`;
    const formField=name=>$('#registro-form').elements.namedItem(name);
    function openEditor(title,html,action,submit='Guardar'){
        $('#detalle').close();$('#editor-titulo').textContent=title;$('#editor-campos').innerHTML=html;$('#form-error').hidden=true;$('#guardar').textContent=submit;$('#guardar').disabled=false;editorAction=action;$('#editor').showModal();
    }
    function patientFields(id,locked){return `<label class="full">Buscar paciente por nombre o DNI<input class="form-control" type="search" id="buscar-paciente" placeholder="Escribe para buscar…" maxlength="100" ${locked?'disabled':''}><span class="gestion-hint">Se muestran hasta 100 coincidencias. Usa la búsqueda para localizar a cualquier paciente.</span></label>${select('pacienteId','Paciente *',[['','Seleccionar paciente']],id,`required ${locked?'disabled':''}`)}`;}
    async function updateOptions(patient='',term='',selected={}){
        const sequence=++optionSequence;$('#guardar').disabled=true;
        try{
            const result=await api(module+'/opciones?'+new URLSearchParams({...(patient?{paciente:patient}:{}),q:term}));
            if(sequence!==optionSequence||!$('#editor').open)return;
            const el=formField('pacienteId');
            el.innerHTML='<option value="">Seleccionar paciente</option>'+result.pacientes.map(p=>`<option value="${p.id}">${escape(p.nombre)}${p.dni?' · '+escape(p.dni):''}${p.activo?'':' (inactivo)'}</option>`).join('');
            el.value=patient;
            const budget=formField('presupuestoId');
            if(budget){budget.innerHTML=`<option value="">${module==='pagos'?'Seleccionar presupuesto aprobado':'Sin presupuesto asociado'}</option>`+result.presupuestos.map(b=>`<option value="${b.id}" data-saldo="${b.saldo}">#${b.id} · ${escape(b.titulo)} · ${module==='pagos'?money(b.saldo):escape(label(b.estado))}</option>`).join('');budget.value=selected.presupuesto||'';}
            const cita=formField('citaId');
            if(cita){cita.innerHTML='<option value="">Sin cita asociada</option>'+result.citas.map(c=>`<option value="${c.id}">#${c.id} · ${date(c.fecha)} · ${escape(c.estado)}</option>`).join('');cita.value=selected.cita||'';}
            updateBalance();
            $('#guardar').disabled=false;
        }catch(error){$('#form-error').textContent=error.message;$('#form-error').hidden=false;}
    }
    function wirePatient(patient,selected={},locked=false){
        let timer;$('#buscar-paciente').addEventListener('input',e=>{clearTimeout(timer);timer=setTimeout(()=>updateOptions('',e.target.value),300);});
        formField('pacienteId').addEventListener('change',e=>updateOptions(e.target.value,$('#buscar-paciente').value));
        if(formField('presupuestoId'))formField('presupuestoId').addEventListener('change',updateBalance);
        return updateOptions(patient,'',selected);
    }
    function updateBalance(){
        const budget=formField('presupuestoId'),hint=$('#saldo-pago');
        if(!hint||!budget)return;
        const balance=budget.selectedOptions[0]?.dataset.saldo;
        hint.textContent=balance!==undefined?'Saldo disponible: '+money(balance):'Selecciona el paciente y un presupuesto aprobado con saldo.';
        if(formField('monto')){formField('monto').max=balance||'';}
    }
    function itemRow(item={}){return `<div class="gestion-item">${input('descripcion','Concepto *','text',item.descripcion||'','required maxlength="250"')}${input('cantidad','Cantidad *','number',item.cantidad??1,'required min="1" max="1000" step="1"')}${input('precio','Precio S/ *','number',item.precio??'','required min="0.01" max="9999999999.99" step="0.01"')}<button type="button" class="gestion-remove" data-remove-item aria-label="Quitar concepto"><i class="bi bi-x"></i></button></div>`;}
    function totalItems(){let cents=0;document.querySelectorAll('.gestion-item').forEach(row=>{const qty=Number(row.querySelector('[name="cantidad"]').value)||0;const price=Number(row.querySelector('[name="precio"]').value)||0;cents+=qty*Math.round(price*100);});$('#total-presupuesto').textContent=money(cents/100);}
    async function editRecord(record=null){
        current=record;const patient=record?.pacienteId||field('paciente').value||params.get('paciente')||'';
        const selected={presupuesto:record?.presupuesto_id||params.get('presupuesto'),cita:record?.cita_id};
        let fields=patientFields(patient,!!record);
        if(module==='presupuestos'){
            fields+=input('titulo','Nombre del presupuesto *','text',record?.titulo||'','required maxlength="180"')+input('vence','Válido hasta *','date',record?.vence||today(),'required min="'+today()+'"')+textarea('notas','Observaciones',record?.notas||'');
            fields=`<div class="gestion-info">Se guardará como borrador. Al aprobarlo se bloquearán los conceptos y se habilitarán los abonos.</div><div class="gestion-form-grid">${fields}</div><section class="gestion-items"><div class="gestion-items-head"><h3>Conceptos del presupuesto</h3><button class="btn btn-outline-secondary btn-sm" type="button" id="agregar-item">+ Agregar concepto</button></div><div id="items">${(record?.items||[{}]).map(itemRow).join('')}</div><div class="gestion-total"><span>Total del presupuesto</span><strong id="total-presupuesto"></strong></div></section>`;
        } else if(module==='pagos'){
            fields+=select('presupuestoId','Presupuesto aprobado *',[],'','required')+input('monto','Importe S/ *','number','','required min="0.01" step="0.01"')+input('fecha','Fecha del cobro *','date',today(),'required max="'+today()+'"')+select('metodo','Método de pago *',['EFECTIVO','TARJETA','TRANSFERENCIA','YAPE','PLIN'].map(v=>[v,label(v)]),'EFECTIVO','required')+input('referencia','Referencia / operación','text','','maxlength="120"');
            fields=`<div class="gestion-info">Registra únicamente dinero ya recibido. Esta acción no carga tarjetas ni transfiere fondos.</div><div class="gestion-form-grid">${fields}</div><p class="gestion-info mt-3 mb-0" id="saldo-pago"></p>`;
        } else if(module==='seguimiento'){
            fields+=input('titulo','Motivo del seguimiento *','text',record?.titulo||'','required maxlength="180"')+input('fecha','Fecha y hora *','datetime-local',record?.fecha?.slice(0,16)||today()+'T09:00','required')+select('canal','Canal de contacto *',['LLAMADA','WHATSAPP','CORREO','PRESENCIAL'].map(v=>[v,label(v)]),record?.canal||'LLAMADA','required')+select('citaId','Cita asociada',[])+select('presupuestoId','Presupuesto asociado',[])+textarea('notas','Indicaciones para el contacto',record?.notas||'');
            fields=`<div class="gestion-info">El seguimiento organiza tareas internas. No envía mensajes ni reserva citas automáticamente.</div><div class="gestion-form-grid">${fields}</div>`;
        } else if(module==='documentos'){
            fields+=input('titulo','Nombre del documento *','text','','required maxlength="180"')+select('tipo','Tipo de documento *',['CONSENTIMIENTO','INFORME','RECETA','COMPROBANTE','OTRO'].map(v=>[v,label(v)]),'INFORME','required')+select('citaId','Cita asociada',[])+select('presupuestoId','Presupuesto asociado',[])+`<label class="full">Archivo *<input class="form-control" type="file" name="archivo" accept=".pdf,.jpg,.jpeg,.png" required><span class="gestion-hint">PDF, JPG o PNG · Máximo 1 MB. Se conserva el original y no se publica en una URL abierta.</span></label>`;
            fields=`<div class="gestion-form-grid">${fields}</div>`;
        }
        const token=module==='pagos'?crypto.randomUUID():null;
        openEditor((record?'Editar ':config.create)+(record?'#'+record.id:''),fields,async()=>{
            const values=Object.fromEntries(new FormData($('#registro-form')));values.pacienteId=Number(formField('pacienteId').value);
            if(module==='documentos'){
                const file=formField('archivo').files[0];if(!file||file.size>1048576)throw new Error('El archivo debe pesar como máximo 1 MB.');
                const body=new FormData($('#registro-form'));for(const key of ['citaId','presupuestoId'])if(!body.get(key))body.delete(key);
                return api('documentos',{method:'POST',body});
            }
            if(module==='presupuestos')values.items=[...document.querySelectorAll('.gestion-item')].map(row=>({descripcion:row.querySelector('[name="descripcion"]').value,cantidad:Number(row.querySelector('[name="cantidad"]').value),precio:row.querySelector('[name="precio"]').value}));
            if(module==='pagos'){values.token=token;values.presupuestoId=Number(values.presupuestoId);}
            if(module==='seguimiento'){values.citaId=values.citaId?Number(values.citaId):null;values.presupuestoId=values.presupuestoId?Number(values.presupuestoId):null;}
            values.version=record?.version??0;
            return api(module+(record?'/'+record.id:''),{method:record?'PUT':'POST',body:values});
        },module==='pagos'?'Confirmar abono':module==='documentos'?'Guardar documento':'Guardar');
        if(module==='presupuestos'){
            $('#agregar-item').onclick=()=>{if(document.querySelectorAll('.gestion-item').length>=100)return;$('#items').insertAdjacentHTML('beforeend',itemRow());totalItems();};
            $('#items').addEventListener('input',totalItems);$('#items').addEventListener('click',e=>{const b=e.target.closest('[data-remove-item]');if(b&&document.querySelectorAll('.gestion-item').length>1){b.closest('.gestion-item').remove();totalItems();}});totalItems();
        }
        if(module==='pagos'){formField('metodo').addEventListener('change',()=>{formField('referencia').required=formField('metodo').value!=='EFECTIVO';});}
        await wirePatient(patient,selected,!!record);
    }
    function links(patient){return `<nav class="gestion-links" aria-label="Información del mismo paciente">${[...allowed].filter(m=>m!==module&&m!=='documentos').map(m=>`<a href="${m==='reportes'?'/interno/reportes':base+m}?paciente=${patient}">${escape({seguimiento:'Seguimiento',presupuestos:'Presupuestos',pagos:'Pagos',reportes:'Reportes'}[m]||m)}</a>`).join('')}</nav>`;}
    const dataPair=(title,value,full=false)=>`<div class="${full?'full':''}"><dt>${escape(title)}</dt><dd>${escape(value??'—')}</dd></div>`;
    function historyHtml(rows){return `<h3>Historial del registro</h3><ol class="gestion-history">${rows.map(h=>`<li><strong>${escape(label(h.accion))}</strong><p>${escape(h.detalle)}</p><small>${date(h.fecha)} · ${escape(h.usuario)}</small></li>`).join('')}</ol>`;}
    async function showDetail(id){
        try{
            const r=await api(module+'/'+id);current=r;
            $('#detalle-titulo').textContent=(module==='pagos'?'Constancia interna de cobro':config.title)+' #'+id;
            let details=dataPair('Paciente',r.paciente)+dataPair('DNI',r.dni||'No registrado')+dataPair('Estado',label(r.estado));
            let extra='';let actions='<button class="btn btn-light" type="button" data-close>Cerrar</button>';
            const actionButton=(action,text,cls='btn-primary')=>`<button class="btn ${cls}" type="button" data-action="${action}">${text}</button>`;
            if(module==='presupuestos'){
                details+=dataPair('Vigencia',date(r.vence))+dataPair('Presupuesto',r.titulo,true)+dataPair('Notas',r.notas||'Sin observaciones',true);
                extra=table(['Concepto','Cantidad','Precio','Subtotal'],r.items.map(i=>[escape(i.descripcion),i.cantidad,money(i.precio),money(i.subtotal)]))+`<div class="gestion-total"><span>Total: ${money(r.total)}</span><span>Abonado: ${money(r.pagado)} · Saldo: ${money(r.saldo)}</span></div>`;
                if(r.estado==='BORRADOR')actions+=actionButton('editar','Editar','btn-outline-secondary')+actionButton('APROBADO','Aprobar')+actionButton('RECHAZADO','Rechazar','btn-outline-secondary');
                if(['BORRADOR','APROBADO'].includes(r.estado)&&Number(r.pagado)===0)actions+=actionButton('ANULADO','Anular','btn-outline-danger');
                if(r.estado==='APROBADO'&&Number(r.saldo)>0&&allowed.has('pagos'))actions+=`<a class="btn btn-primary" href="${base}pagos?paciente=${r.pacienteId}&presupuesto=${id}&nuevo=1">Registrar abono</a>`;
                actions+=actionButton('imprimir','Imprimir','btn-outline-secondary');
            }else if(module==='pagos'){
                details+=dataPair('Fecha del cobro',date(r.fecha))+dataPair('Presupuesto','#'+r.presupuesto_id+' · '+r.presupuesto)+dataPair('Importe recibido',money(r.monto))+dataPair('Método',label(r.metodo))+dataPair('Referencia',r.referencia||'Sin referencia')+dataPair('Registrado por',r.creado_por);
                if(r.estado==='ANULADO')details+=dataPair('Motivo de anulación',r.motivo_anulacion,true);
                extra='<p class="gestion-info">Constancia de registro interno. No sustituye una boleta, factura ni comprobante tributario.</p>';
                if(r.estado==='REGISTRADO')actions+=actionButton('ANULADO','Anular cobro','btn-outline-danger');
                actions+=actionButton('imprimir','Imprimir constancia','btn-outline-secondary');
            }else if(module==='seguimiento'){
                details+=dataPair('Programado',date(r.fecha))+dataPair('Canal',label(r.canal))+dataPair('Motivo',r.titulo,true)+dataPair('Indicaciones',r.notas||'Sin indicaciones',true)+dataPair('Resultado',r.resultado||'Pendiente de contacto',true);
                if(r.cita_id)details+=dataPair('Cita asociada','#'+r.cita_id+' · '+date(r.citaFecha));
                if(r.presupuesto_id)details+=dataPair('Presupuesto asociado','#'+r.presupuesto_id+' · '+r.presupuesto);
                if(r.estado==='PENDIENTE')actions+=actionButton('editar','Editar','btn-outline-secondary')+actionButton('COMPLETADO','Completar seguimiento')+actionButton('CANCELADO','Cancelar seguimiento','btn-outline-danger');
            }else if(module==='documentos'){
                details+=dataPair('Tipo',label(r.tipo))+dataPair('Documento',r.titulo,true)+dataPair('Archivo',r.nombre_archivo)+dataPair('Tamaño',(r.tamano/1024).toFixed(1)+' KB')+dataPair('Adjuntado por',r.creado_por)+dataPair('Fecha',date(r.creado_en));
                if(r.presupuesto_id)details+=dataPair('Presupuesto asociado','#'+r.presupuesto_id+' · '+r.presupuesto);
                if(r.cita_id)details+=dataPair('Cita asociada','#'+r.cita_id);
                actions+=`<a class="btn btn-primary" href="${base}documentos/${id}/archivo">Descargar archivo</a>`;
                actions+=actionButton(r.estado==='ACTIVO'?'ARCHIVADO':'ACTIVO',r.estado==='ACTIVO'?'Archivar':'Restaurar','btn-outline-secondary');
            }
            $('#detalle-contenido').innerHTML=`<div class="gestion-print-head">DentIA · Gestión interna · Generado: ${date(today())}</div><dl class="gestion-detail-grid">${details}</dl>${links(r.pacienteId)}${extra}${historyHtml(r.historial)}`;
            $('#detalle-acciones').innerHTML=actions;$('#detalle').showModal();
        }catch(error){notice(error.message,true);}
    }
    function changeState(state){
        const r=current;const isApproval=state==='APROBADO';
        const description=module==='pagos'?'El cobro permanecerá en el historial y su importe volverá al saldo pendiente. Esta acción no devuelve dinero al paciente.':state==='APROBADO'?'Al aprobar, los conceptos e importes quedarán bloqueados y se habilitará el registro de abonos.':module==='documentos'?'El archivo se conserva. El cambio de estado quedará registrado en el historial.':'El cambio quedará registrado con tu usuario y no podrá deshacerse desde esta pantalla.';
        openEditor(label(state)+' · Registro #'+r.id,`<p class="gestion-info">${description}</p>${textarea('motivo',isApproval?'Observación (opcional)':state==='COMPLETADO'?'Resultado del contacto *':'Motivo *','','maxlength="1000" '+(isApproval?'':'required'))}`,()=>api(module+'/'+r.id+'/'+(module==='pagos'?'anular':'estado'),{method:'POST',body:{estado:state,motivo:formField('motivo').value,version:r.version}}),'Confirmar');
    }
    $('#registro-form').addEventListener('submit',async e=>{
        e.preventDefault();if(busy||!$('#registro-form').reportValidity())return;
        busy=true;$('#guardar').disabled=true;$('#form-error').hidden=true;
        try{await editorAction();$('#editor').close();optionSequence++;notice('Operación guardada. Los datos relacionados se actualizaron correctamente.');await load();}
        catch(error){$('#form-error').textContent=error.message;$('#form-error').hidden=false;}
        finally{busy=false;$('#guardar').disabled=false;}
    });
    document.addEventListener('click',e=>{
        const close=e.target.closest('[data-close]');if(close&&!busy){close.closest('dialog').close();optionSequence++;}
        const detail=e.target.closest('[data-detail]');if(detail)showDetail(Number(detail.dataset.detail));
        const action=e.target.closest('[data-action]');if(action){const a=action.dataset.action;if(a==='editar')editRecord(current);else if(a==='imprimir')window.print();else changeState(a);}
    });
    $('#editor').addEventListener('cancel',e=>{if(busy)e.preventDefault();else optionSequence++;});
    filters.addEventListener('submit',async e=>{e.preventDefault();if(!filters.reportValidity())return;page=0;try{await filterPatients(field('paciente').value,field('q').value);await load();}catch(error){notice(error.message,true);}});
    filters.addEventListener('reset',()=>setTimeout(async()=>{page=0;if(module==='reportes'){field('desde').value=today().slice(0,8)+'01';field('hasta').value=today();}try{await filterPatients('');await load();}catch(error){notice(error.message,true);}},0));
    $('#anterior').onclick=()=>{if(page>0){page--;load();}};$('#siguiente').onclick=()=>{if(page+1<pages){page++;load();}};
    $('#actualizar').onclick=load;
    $('#crear').onclick=async()=>{
        if(module!=='reportes'){await editRecord();return;}
        const button=$('#crear');button.disabled=true;
        try{
            const response=await fetch(base+'reportes/cobros.csv?'+query(),{credentials:'same-origin',cache:'no-store'});
            if(!response.ok||!response.headers.get('content-type')?.includes('csv')){let msg='No se pudo exportar. Revisa tu sesión y el rango de fechas.';if(response.headers.get('content-type')?.includes('json'))msg=(await response.json()).error||msg;throw new Error(msg);}
            const url=URL.createObjectURL(await response.blob());const a=document.createElement('a');a.href=url;a.download='dentia-cobros.csv';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
        }catch(error){notice(error.message,true);}finally{button.disabled=false;}
    };
    (async()=>{try{await filterPatients(params.get('paciente'));await load();if(/^\d+$/.test(params.get('registro')||''))await showDetail(Number(params.get('registro')));if(params.get('nuevo')==='1'&&module!=='reportes'&&!$('#crear').disabled)await editRecord();}catch(error){notice(error.message,true);}})();
})();
