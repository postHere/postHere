const profileImageContainer = document.getElementById('profileImageContainer');
const profileImageInput = document.getElementById('profileImageInput');
const profileImage = document.getElementById('profileImage');
const saveButton = document.getElementById('saveButton');
const statusMessage = document.getElementById('statusMessage');

// 1. 프로필 이미지 영역 클릭 시, 숨겨진 파일 입력 필드 클릭
profileImageContainer.addEventListener('click', () => {
    profileImageInput.click();
});

// 2. 사용자가 파일을 선택했을 때의 처리
profileImageInput.addEventListener('change', (event) => {
    const file = event.target.files[0];
    if (!file) {
        return; // 파일 선택 취소 시 아무것도 하지 않음
    }

    const reader = new FileReader();
    reader.onload = (e) => {
        profileImage.src = e.target.result;
    };
    reader.readAsDataURL(file);

    // 'Save Changes' 버튼을 보여줌 (클래스명 변경에 따라 수정)
    saveButton.classList.add('visible');
    statusMessage.textContent = '';
});

// 3. 'Save Changes' 버튼 클릭 시, 서버로 이미지 전송
saveButton.addEventListener('click', async () => {
    const file = profileImageInput.files[0];
    if (!file) {
        alert('Please select an image file.');
        return;
    }

    const formData = new FormData();
    formData.append('profileImage', file);

    saveButton.disabled = true;
    saveButton.textContent = 'Saving...';
    statusMessage.textContent = '';

    try {
        const response = await fetch('/api/profile/image', {
            method: 'POST',
            body: formData,
        });

        if (!response.ok) {
            throw new Error('Image upload failed. Please try again.');
        }

        const result = await response.json();

        statusMessage.textContent = 'Profile image updated successfully!';
        statusMessage.style.color = 'rgb(34 197 94)';

        // 버튼 숨기기 (클래스명 변경에 따라 수정)
        saveButton.classList.remove('visible');

    } catch (error) {
        statusMessage.textContent = error.message;
        statusMessage.style.color = 'rgb(239 68 68)';
    } finally {
        saveButton.disabled = false;
        saveButton.textContent = 'Save Changes';
    }
});