document.addEventListener("DOMContentLoaded", function(){
    document.querySelectorAll('.dropdown-hover').forEach(drop => {
        drop.addEventListener('mouseenter', function(){
            const toggle = this.querySelector('[data-bs-toggle="dropdown"]');
            bootstrap.Dropdown.getOrCreateInstance(toggle).show();
        });

        drop.addEventListener('mouseleave', function(){
            const toggle = this.querySelector('[data-bs-toggle="dropdown"]');
            bootstrap.Dropdown.getOrCreateInstance(toggle).hide();
        });
    });
});
