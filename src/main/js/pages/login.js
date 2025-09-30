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
                // const message = xhr.responseJSON.message;
                // loginError.text(message);

                console.error("AJAX Error Status:", status);
                console.error("AJAX Error Object:", JSON.stringify(error, null, 2));
                console.error("Full XHR Object:", JSON.stringify(xhr, null, 2))

                let message = "알 수 없는 오류가 발생했습니다. 네트워크 연결을 확인해 주세요.";

                // 서버에서 보낸 JSON 응답이 있을 경우에만 message를 파싱합니다.
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                } else if (xhr.responseText) {
                    // JSON이 아닌 다른 텍스트 응답이 있을 수도 있습니다.
                    message = xhr.responseText;
                }

                loginError.text(message);
            });
    }

    checkAutoLogin();
    $('form').on('submit', tryLogin)
}