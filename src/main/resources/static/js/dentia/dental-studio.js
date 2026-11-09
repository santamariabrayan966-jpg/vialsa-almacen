(() => {
    const $ = selector => document.querySelector(selector);
    const upper = [18,17,16,15,14,13,12,11,21,22,23,24,25,26,27,28];
    const lower = [48,47,46,45,44,43,42,41,31,32,33,34,35,36,37,38];
    const poses = new Map(); let selected = 26;
    const names = ['','Incisivo central','Incisivo lateral','Canino','Primer premolar','Segundo premolar','Primer molar','Segundo molar','Tercer molar'];
    function select(number) {
        selected = number; $('#selected-tooth').textContent = 'FDI ' + number;
        $('#selected-name').textContent = names[number % 10] + (number < 30 ? ' superior' : ' inferior') + ([1,4].includes(Math.floor(number / 10)) ? ' derecho' : ' izquierdo');
        $('#inclination').value = poses.get(number) || 0; $('#inclination-value').textContent = (poses.get(number) || 0) + '°';
        document.querySelectorAll('#teeth-grid button').forEach(b => b.classList.toggle('active', Number(b.dataset.tooth) === number));
    }
    const clinicalPreview = new URLSearchParams(location.search).get('visual') === 'clinico';
    if (clinicalPreview) {
        $('.version').textContent = 'Modelo articulado · prueba clínica';
        $('.download').href = '/models/dental/dentia-articulado-v2-clinico.glb';
    }
    const viewer = new DentiaDentalViewer({clinicalPreview,canvas:$('#dental-canvas'),container:$('#viewport'),onSelect:select,onStatus:(message,status)=>{ $('#model-status').textContent=message; $('#model-status').dataset.status=status; }});
    $('#teeth-grid').innerHTML = [...upper,...lower].map(number => `<button type="button" data-tooth="${number}" aria-label="Seleccionar pieza FDI ${number}">${number}</button>`).join('');
    $('#teeth-grid').addEventListener('click',e=>{ const button=e.target.closest('[data-tooth]');if(button)viewer.select(Number(button.dataset.tooth)); });
    $('#opening').addEventListener('input',e=>{viewer.setOpening(e.target.value/100);$('#opening-value').textContent=e.target.value+'%';});
    ['gums','roots','additional','wireframe'].forEach(id=>$('#'+id).addEventListener('change',e=>viewer.setOptions({[id]:e.target.checked})));
    $('#arch').addEventListener('change',e=>viewer.setOptions({arch:e.target.value}));
    document.querySelectorAll('[data-view]').forEach(button=>button.addEventListener('click',()=>{
        viewer.setView(button.dataset.view);document.querySelectorAll('[data-view]').forEach(b=>b.classList.toggle('active',b===button));
    }));
    $('#zoom-in').addEventListener('click',()=>viewer.zoom(.85));$('#zoom-out').addEventListener('click',()=>viewer.zoom(1.18));
    $('#reset-view').addEventListener('click',()=>viewer.setView('three-quarter'));
    const updatePose=value=>{poses.set(selected,Number(value));$('#inclination-value').textContent=value+'°';viewer.setData([],Array.from(poses,([numeroPieza,inclinacion])=>({numeroPieza,inclinacion})));};
    $('#inclination').addEventListener('input',e=>updatePose(e.target.value));
    $('#reset-tooth').addEventListener('click',()=>{updatePose(0);$('#inclination').value=0;});
    viewer.ready.then(()=>viewer.select(26)).catch(error=>console.error('DentIA:',error));
    window.addEventListener('pagehide',()=>viewer.dispose(),{once:true});
})();
