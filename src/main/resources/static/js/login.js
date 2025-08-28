$(document).ready(function () {

    $('form').on('submit', tryLogin)
});

function tryLogin(event) {

    event.preventDefault();
    $('#login-error').text('');

    const username = $('#id').val();
    const password = $('#password').val();

    if (!username || !password) {

        const message = '아이디와 비밀번호를 모두 입력해주세요';
        $('#login-error').text(message);
        return false;
    }

    const loginData = {
        id: username,
        password: password
    };

    $.ajax({
        type: "POST",
        url: "/login",
        data: loginData,
        datatype: "json"
    })
        .done(function (response) {
            window.location.href = "/";
        })
        .fail(function (xhr, status, error) {

            const message = xhr.responseJSON.message;
            $('#login-error').text(message);
        });
}
