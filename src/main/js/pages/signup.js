export function initSignup() {

// --- 사용할 HTML 요소들을 미리 변수에 할당 ---
    const emailInput = document.getElementById('email');
    const checkEmailBtn = document.getElementById('checkEmailBtn');
    const emailFeedback = document.getElementById('emailFeedback');
    const sendCodeBtn = document.getElementById('sendCodeBtn');
    const verificationSection = document.getElementById('verificationSection');
    const verificationCodeInput = document.getElementById('verificationCode');
    const timerDisplay = document.getElementById('timer');
    const verifyCodeBtn = document.getElementById('verifyCodeBtn');
    const codeFeedback = document.getElementById('codeFeedback');
    const resendCodeBtn = document.getElementById('resendCodeBtn');
    const userInfoSection = document.getElementById('userInfoSection');
    const passwordInput = document.getElementById('password');
    const passwordCheckInput = document.getElementById('passwordCheck');
    const passwordFeedback = document.getElementById('passwordFeedback');
    const nicknameInput = document.getElementById('nickname');
    const checkNicknameBtn = document.getElementById('checkNicknameBtn');
    const nicknameFeedback = document.getElementById('nicknameFeedback');
    const signupBtn = document.getElementById('signupBtn');
    const signupForm = document.getElementById('signupForm');

// 새로 추가된 요소 변수에 할당
    const verificationInputs = document.getElementById('verificationSection');

// --- 회원가입 단계별 성공 여부를 저장하는 변수 ---
    let isEmailValid = false;
    let isEmailVerified = false;
    let isPasswordValid = false;
    let isNicknameValid = false;

// --- 타이머 관련 변수 ---
    let timer;
    let timeRemaining = 180; // 3분

// --- 자주 사용하는 함수들 ---
    function setFeedback(element, message, type) {
        element.textContent = message;
        element.classList.add('active'); // 메시지를 보이게 함
        element.classList.remove('feedback-success', 'feedback-error');
        element.classList.add(`feedback-${type}`);
    }

    function updateSignupButtonState() {
        // 모든 유효성 검사가 통과되면 '가입하기' 버튼 활성화
        if (isEmailValid && isEmailVerified && isPasswordValid && isNicknameValid) {
            signupBtn.disabled = false;
        } else {
            // 유효성 조건이 하나라도 불충분하면 비활성화
            signupBtn.disabled = true;
        }
    }

    function startTimer() {
        clearInterval(timer);
        timeRemaining = 180;
        timerDisplay.style.display = 'block';
        resendCodeBtn.classList.add('hidden');
        verifyCodeBtn.disabled = true;
        verificationCodeInput.value = '';
        timer = setInterval(() => {
            const minutes = Math.floor(timeRemaining / 60);
            const seconds = timeRemaining % 60;
            timerDisplay.textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`;
            timeRemaining--;

            if (timeRemaining < 0) {
                clearInterval(timer);
                timerDisplay.style.display = 'none';
                setFeedback(codeFeedback, '인증 유효 시간이 초과되었습니다.', 'error');
                verifyCodeBtn.disabled = true;
                resendCodeBtn.classList.remove('hidden');
            }
        }, 1000);
    }

// --- 각 버튼과 입력창에 대한 이벤트 리스너 설정 ---
// 인증코드 입력란이 공란일시 '확인' 버튼은 비활성화 상태여야 한다.
    verificationCodeInput.addEventListener('input', () => {
        const value = verificationCodeInput.value.trim();
        verifyCodeBtn.disabled = value.length === 0;
    });

    // '인증 코드 입력란'에 입력이 있을 때 이메일 피드백 메시지 초기화
    verificationCodeInput.addEventListener('input', () => {
        emailFeedback.classList.remove('active', 'feedback-success', 'feedback-error');
        emailFeedback.textContent = '';
    });

    // 이메일 입력 필드에 입력이 있을 때 버튼 활성화 및 메시지 초기화
    emailInput.addEventListener('input', () => {
        const value = emailInput.value.trim();
        if (value.length > 0) {
            checkEmailBtn.disabled = false;
            checkEmailBtn.classList.add('active');
        } else {
            checkEmailBtn.disabled = true;
            checkEmailBtn.classList.remove('active');
        }

        // 입력란을 수정할 때 메시지 및 유효성 클래스 초기화
        emailFeedback.classList.remove('active', 'feedback-success', 'feedback-error');
        emailFeedback.textContent = '';
        emailInput.classList.remove('valid', 'invalid');

        // 이메일 입력 수정 시 인증 섹션 숨기기
        verificationSection.classList.add('hidden');
    });

    // 닉네임 입력 필드에 입력이 있을 때!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    nicknameInput.addEventListener('input', () => {
        // 닉네임 입력값을 수정하거나 지우면 유효성 상태 초기화
        isNicknameValid = false;

        // 피드백 메시지 숨기기
        nicknameFeedback.classList.remove('active', 'feedback-success', 'feedback-error');
        nicknameFeedback.textContent = '';

        // 최종 가입하기 버튼 상태 업데이트
        updateSignupButtonState();
    });

    // 1. 이메일 중복 확인 (서버 API 호출)
    checkEmailBtn.addEventListener('click', async () => {
        const email = emailInput.value;

        // 버튼을 누를 때 피드백 메시지를 즉시 초기화
        emailFeedback.classList.remove('active', 'feedback-success', 'feedback-error');
        emailFeedback.textContent = '';
        emailInput.classList.remove('valid', 'invalid');

        if (!email || !email.includes('@')) {
            setFeedback(emailFeedback, '이메일 형식이 올바르지 않습니다.', 'error');
            emailInput.classList.add('invalid');
            return;
        }

        try {
            const response = await fetch(`/check-email?email=${encodeURIComponent(email)}`);
            if (!response.ok) throw new Error('Server error');
            const data = await response.json();

            if (data.available) {
                setFeedback(emailFeedback, '사용 가능한 이메일입니다.', 'success');

                emailInput.classList.add('valid');
                isEmailValid = true;

                // 다음 단계로 넘어가기 위한 작업
                checkEmailBtn.classList.add('hidden'); // 중복 확인 버튼 숨기기
                sendCodeBtn.classList.remove('hidden'); // '인증 코드 전송' 버튼 표시
                emailInput.readOnly = true; // 이메일 입력란 수정 불가능하게 설정
            } else {
                setFeedback(emailFeedback, '이미 사용 중인 이메일입니다.', 'error');
                emailInput.classList.add('invalid');
                isEmailValid = false;
            }
        } catch (error) {
            setFeedback(emailFeedback, '오류가 발생했습니다. 다시 시도해주세요.', 'error');
            emailInput.classList.add('invalid');
            isEmailValid = false;
        }
        updateSignupButtonState();
    });

// 2. 인증 코드 전송 (서버 API 호출)
    sendCodeBtn.addEventListener('click', async () => {
        // --- 이메일 피드백 메시지 초기화 추가 ---
        emailFeedback.classList.remove('active', 'feedback-success', 'feedback-error');
        emailFeedback.textContent = '';

        // 이메일 입력란을 읽기 전용으로 변경
        emailInput.readOnly = true;

        try {
            const response = await fetch('/send-verification', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: `email=${encodeURIComponent(emailInput.value)}`
            });
            if (!response.ok) throw new Error('Server error');
            const data = await response.json();

            if (data.success) {
                verificationInputs.classList.remove('hidden');
                sendCodeBtn.classList.add('hidden');
                startTimer();
                setFeedback(codeFeedback, '입력하신 이메일로 인증 코드가 발송되었습니다.', 'success');
            } else {
                setFeedback(emailFeedback, '인증 코드 전송에 실패했습니다. 다시 시도해 주세요.', 'error');
                emailInput.readOnly = false;
            }
        } catch (error) {
            setFeedback(emailFeedback, '인증 코드를 전송하는 중 오류가 발생했습니다.', 'error');
            emailInput.readOnly = false;
        }
    });

    resendCodeBtn.addEventListener('click', () => sendCodeBtn.click());

// 3. 인증 코드 확인 (서버 API 호출)
    verifyCodeBtn.addEventListener('click', async () => {
        const code = verificationCodeInput.value.trim();
        if (!code) {
            setFeedback(codeFeedback, '인증 코드를 입력해주세요.', 'error');
            alert("인증에 실패하였습니다")
            return;
        }

        try {
            const response = await fetch('/verify-code', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: `code=${encodeURIComponent(code)}`
            });

            if (!response.ok) throw new Error('Server error');
            const data = await response.json();

            if (data.valid) {
                clearInterval(timer);
                setFeedback(codeFeedback, '이메일 인증이 완료되었습니다.', 'success');
                isEmailVerified = true;
                emailInput.readOnly = true;

                // 인증 성공 시 관련 필드 비활성화
                [checkEmailBtn, sendCodeBtn, verificationCodeInput, verifyCodeBtn].forEach(el => el.disabled = true);
                resendCodeBtn.classList.add('hidden');
                timerDisplay.style.display = 'none';

                userInfoSection.classList.remove('hidden'); // 다음 단계 표시

                // 비밀번호 단계가 나타날 때 버튼 상태 업데이트
                updateSignupButtonState();

                // 인증 성공 시 이메일 섹션 숨기기
                const emailSection = document.getElementById('emailSection');
                emailSection.classList.add('hidden');

            } else {
                setFeedback(codeFeedback, '잘못된 인증 코드입니다. 다시 시도해주세요.', 'error');
                isEmailVerified = false;
                resendCodeBtn.classList.remove('hidden'); // 추가된 코드

            }
        } catch (error) {
            setFeedback(codeFeedback, '인증 과정에서 오류가 발생했습니다.', 'error');
            isEmailVerified = false;
            // 인증 실패 시 재전송 버튼 보이기
            resendCodeBtn.classList.remove('hidden');
        }
        updateSignupButtonState();
    });

// 4. 비밀번호 일치 확인 (자체 로직)
// 비밀번호 유효성 검사 (길이 조건 추가)
    function validatePassword() {
        const password = passwordInput.value;
        const passwordCheck = passwordCheckInput.value;

// 조건 1: 비밀번호 길이 검사 (5자리 이상, 15자리 미만)
        if (password && (password.length < 5 || password.length >= 15)) {
            setFeedback(passwordFeedback, '비밀번호는 5자 이상 14자 이하로 입력해주세요.', 'error');
            isPasswordValid = false;
            updateSignupButtonState();
            return false; // 유효성 검사 실패
        }

// 조건 2: 두 비밀번호 필드가 모두 채워져 있을 때 일치 여부 검사
        if (password && passwordCheck) {
            if (password === passwordCheck) {
                setFeedback(passwordFeedback, '비밀번호가 일치합니다.', 'success');
                isPasswordValid = true;
            } else {
                setFeedback(passwordFeedback, '비밀번호가 일치하지 않습니다.', 'error');
                isPasswordValid = false;
            }
        } else {
// 하나의 필드만 채워져 있거나 둘 다 비어있을 경우
            setFeedback(passwordFeedback, '', 'success'); // 메시지 초기화
            isPasswordValid = false;
        }

        updateSignupButtonState();
        return isPasswordValid; // 최종 유효성 검사 결과 반환
    }

    passwordInput.addEventListener('input', validatePassword);
    passwordCheckInput.addEventListener('input', validatePassword);

// 5. 닉네임 중복 확인 (서버 API 호출)
    checkNicknameBtn.addEventListener('click', async () => {
        const nickname = nicknameInput.value;
        if (!nickname) {
            setFeedback(nicknameFeedback, '사용할 닉네임을 입력해 주세요.', 'error');
            return;
        }

        // 이메일과 마찬가지로 중복 확인 버튼 클릭 시 피드백 메시지 초기화!!!!!!!!!!!!!!!!!!!!!!!!!1
        nicknameFeedback.classList.remove('active', 'feedback-success', 'feedback-error');
        nicknameFeedback.textContent = '';

        try {
            const response = await fetch(`/check-nickname?nickname=${encodeURIComponent(nickname)}`);
            if (!response.ok) throw new Error('Server error');
            const data = await response.json();

            if (data.available) {
                setFeedback(nicknameFeedback, '사용 가능한 닉네임입니다.', 'success');
                isNicknameValid = true;
            } else {
                setFeedback(nicknameFeedback, '이미 사용 중인 닉네임입니다.', 'error');
                isNicknameValid = false;
            }
        } catch (error) {
            setFeedback(nicknameFeedback, '오류가 발생했습니다. 다시 시도해 주세요.', 'error');
            isNicknameValid = false;
        }
        updateSignupButtonState();
    });

// 6. 최종 폼 전송
    signupForm.addEventListener('submit', (e) => {
// 폼 제출 직전, 비밀번호 유효성을 다시 한번 검사
        if (!validatePassword()) {
            e.preventDefault(); // 폼 전송 막기
// alert창을 띄워 사용자에게 알림
            alert('비밀번호는 5자 이상 14자 이하로 입력해 주세요.');
            return;
        }
        if (!isEmailValid || !isEmailVerified || !isPasswordValid || !isNicknameValid) {
            e.preventDefault(); // 폼 전송을 막음
            alert('모든 항목을 올바르게 작성해 주세요.');
        }
    });
}