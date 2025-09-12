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
        element.className = `feedback-message feedback-${type}`;
    }

    function updateSignupButtonState() {
        signupBtn.disabled = !(isEmailValid && isEmailVerified && isPasswordValid && isNicknameValid);
    }

    function startTimer() {
        clearInterval(timer);
        timeRemaining = 180;
        timerDisplay.style.display = 'block';
        resendCodeBtn.classList.add('hidden');
        verifyCodeBtn.disabled = false;
        verificationCodeInput.value = '';

        timer = setInterval(() => {
            const minutes = Math.floor(timeRemaining / 60);
            const seconds = timeRemaining % 60;
            timerDisplay.textContent = `${minutes}:${seconds.toString().padStart(2, '0')}`;
            timeRemaining--;

            if (timeRemaining < 0) {
                clearInterval(timer);
                timerDisplay.style.display = 'none';
                setFeedback(codeFeedback, 'Verification time expired.', 'error');
                verifyCodeBtn.disabled = true;
                resendCodeBtn.classList.remove('hidden');
            }
        }, 1000);
    }

// --- 각 버튼과 입력창에 대한 이벤트 리스너 설정 ---

// 1. 이메일 중복 확인 (서버 API 호출)
    checkEmailBtn.addEventListener('click', async () => {
        const email = emailInput.value;
        if (!email || !email.includes('@')) {
            setFeedback(emailFeedback, 'Please enter a valid email format.', 'error');
            return;
        }

        try {
            const response = await fetch(`/api/check-email?email=${encodeURIComponent(email)}`);
            if (!response.ok) throw new Error('Server error');
            const data = await response.json();

            if (data.available) {
                setFeedback(emailFeedback, 'This email is available.', 'success');
                isEmailValid = true;
                sendCodeBtn.disabled = false;
            } else {
                setFeedback(emailFeedback, 'This email is already in use.', 'error');
                isEmailValid = false;
                sendCodeBtn.disabled = true;
            }
        } catch (error) {
            setFeedback(emailFeedback, 'An error occurred. Please try again.', 'error');
            isEmailValid = false;
            sendCodeBtn.disabled = true;
        }
        updateSignupButtonState();
    });

// 2. 인증 코드 전송 (서버 API 호출)
    sendCodeBtn.addEventListener('click', async () => {
        sendCodeBtn.disabled = true;
        sendCodeBtn.textContent = 'Sending...';

        try {
            const response = await fetch('/api/send-verification', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: `email=${encodeURIComponent(emailInput.value)}`
            });
            if (!response.ok) throw new Error('Server error');
            const data = await response.json();

            if (data.success) {
                verificationSection.classList.remove('hidden');
                setFeedback(codeFeedback, 'A verification code has been sent to your email.', 'success');
                startTimer();
            } else {
                setFeedback(emailFeedback, 'Failed to send code. Please try again.', 'error');
            }
        } catch (error) {
            setFeedback(emailFeedback, 'An error occurred while sending the code.', 'error');
        } finally {
            sendCodeBtn.textContent = 'Send Verification Code';
            // 성공 시에는 어차피 비활성화되므로, 실패 시에만 다시 활성화되도록 로직 구성 가능
        }
    });

    resendCodeBtn.addEventListener('click', () => sendCodeBtn.click());

// 3. 인증 코드 확인 (서버 API 호출)
    verifyCodeBtn.addEventListener('click', async () => {
        const code = verificationCodeInput.value;
        if (!code) {
            setFeedback(codeFeedback, 'Please enter the verification code.', 'error');
            alert("인증에 실패하였습니다")
            return;
        }

        try {
            const response = await fetch('/api/verify-code', {
                method: 'POST',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                body: `code=${encodeURIComponent(code)}`
            });
            if (!response.ok) throw new Error('Server error');
            const data = await response.json();

            if (data.valid) {
                clearInterval(timer);
                setFeedback(codeFeedback, 'Email verification successful.', 'success');
                alert("인증에 성공하였습니다")
                isEmailVerified = true;
                emailInput.readOnly = true;
                // 인증 성공 시 관련 필드 비활성화
                [checkEmailBtn, sendCodeBtn, verificationCodeInput, verifyCodeBtn].forEach(el => el.disabled = true);
                resendCodeBtn.classList.add('hidden');
                timerDisplay.style.display = 'none';

                userInfoSection.classList.remove('hidden'); // 다음 단계 표시
            } else {
                setFeedback(codeFeedback, 'Incorrect code. Please try again.', 'error');
                isEmailVerified = false;
            }
        } catch (error) {
            setFeedback(codeFeedback, 'An error occurred during verification.', 'error');
            isEmailVerified = false;
        }
        updateSignupButtonState();
    });

// 4. 비밀번호 일치 확인 (자체 로직)
// ✅ [수정] 4. 비밀번호 유효성 검사 (길이 조건 추가)
    function validatePassword() {
        const password = passwordInput.value;
        const passwordCheck = passwordCheckInput.value;

        // 조건 1: 비밀번호 길이 검사 (5자리 이상, 15자리 미만)
        if (password && (password.length < 5 || password.length >= 15)) {
            setFeedback(passwordFeedback, 'Password must be between 5 and 14 characters.', 'error');
            isPasswordValid = false;
            updateSignupButtonState();
            return false; // 유효성 검사 실패
        }

        // 조건 2: 두 비밀번호 필드가 모두 채워져 있을 때 일치 여부 검사
        if (password && passwordCheck) {
            if (password === passwordCheck) {
                setFeedback(passwordFeedback, 'Passwords match.', 'success');
                isPasswordValid = true;
            } else {
                setFeedback(passwordFeedback, 'Passwords do not match.', 'error');
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
            setFeedback(nicknameFeedback, 'Please enter a nickname.', 'error');
            return;
        }

        try {
            const response = await fetch(`/api/check-nickname?nickname=${encodeURIComponent(nickname)}`);
            if (!response.ok) throw new Error('Server error');
            const data = await response.json();

            if (data.available) {
                setFeedback(nicknameFeedback, 'This nickname is available.', 'success');
                isNicknameValid = true;
            } else {
                setFeedback(nicknameFeedback, 'This nickname is already in use.', 'error');
                isNicknameValid = false;
            }
        } catch (error) {
            setFeedback(nicknameFeedback, 'An error occurred. Please try again.', 'error');
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
            alert('Password must be between 5 and 14 characters.');
            return;
        }

        if (!isEmailValid || !isEmailVerified || !isPasswordValid || !isNicknameValid) {
            e.preventDefault(); // 폼 전송을 막음
            alert('Please complete all steps correctly.');
        }
    });
}