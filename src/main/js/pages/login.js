import {initBackgroundGeolocation} from "../modules/location-tracker";
import {Preferences} from "@capacitor/preferences";

export function initLogin() {

    const loginError = $('#login-error');

    async function checkAutoLogin() {

        console.log("자동 로그인 요청");
        const response = await fetch('/api/auth/status');
        if (!response.ok) {
            // 서버에서 에러 응답이 온 경우, 그냥 로그인 폼을 보여줍니다.
            console.error('인증 상태 확인 API 에러:', response.status);
            return;
        }

        const result = await response.json();

        // 서버가 보내준 WrapperDTO의 data 필드 안을 확인합니다.
        if (result && result.data && result.data.isAuthenticated) {
            // 이미 로그인된 상태라면 메인 페이지로 바로 이동시킵니다.
            console.log("자동 로그인 성공");
            window.location.replace('/forumMain');
        }
        // 로그인되어 있지 않다면, 아무것도 하지 않고 로그인 폼을 그대로 보여줍니다
    }

    function tryLogin(event) {

        console.log("로그인 요청");
        event.preventDefault();
        loginError.text('');

        const username = $('#id').val();
        const password = $('#password').val();

        if (!username || !password) {

            const message = '아이디와 비밀번호를 모두 입력해주세요';
            loginError.text(message);
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
            dataType: "json"
        })
            .done(async function (data, textStatus, jqXHR) {

                const locationHeader = jqXHR.getResponseHeader('Location');
                const username = data?.data?.username;

                await Preferences.set({
                    key: 'user',
                    value: username
                });

                await initBackgroundGeolocation();
                window.location.replace(locationHeader);
            })
            .fail(function (xhr, status, error) {
                const message = xhr.responseJSON.message;
                loginError.text(message);
            });
    }

    checkAutoLogin();
    $('form').on('submit', tryLogin)
}