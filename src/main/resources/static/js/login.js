$(document).ready(function () {

    $('form').on('submit', function (event) {

        event.preventDefault();
        $('#login-error').text('');

        const username = $('#id').val();
        const password = $('#password').val();

        const loginData = {
            id: username,
            password: password
        };

        $.ajax({
            type: "POST",
            url: "/login",
            data: loginData,
        })
            .done(function (response) {
                window.location.href = "/";
            })
            .fail(function (xhr, status, error) {
                $('#login-error').text(message);
            });
    });
});