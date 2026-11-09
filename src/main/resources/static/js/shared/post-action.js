window.submitProtectedAction = function (url, question) {
    if (!window.confirm(question)) return;
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const parameter = document.querySelector('meta[name="_csrf_parameter"]')?.content;
    if (!token || !parameter) return;
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = url;
    const csrf = document.createElement('input');
    csrf.type = 'hidden';
    csrf.name = parameter;
    csrf.value = token;
    form.appendChild(csrf);
    document.body.appendChild(form);
    form.submit();
};
