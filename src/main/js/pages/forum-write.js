export function initForumWrite() {

    const submitButton = document.getElementById('submit-btn');
    const imageInput = document.getElementById('images');
    const addImageButton = document.getElementById('add-image-btn');
    const imagePreviewContainer = document.getElementById('image-preview-container');
    const contentInput = document.getElementById('content');

    // 이미지 편집기 관련 DOM 요소
    const imageEditorOverlay = document.getElementById('image-editor-overlay');
    const editorCloseBtn = document.getElementById('editor-close-btn');
    const editorDoneBtn = document.getElementById('editor-done-btn');
    const imageToCrop = document.getElementById('image-to-crop');
    const imageCropperContainer = document.getElementById('image-cropper-container');

    // 편집된 이미지 파일을 저장할 변수
    let editedImageFile = null;
    let originalImageFile = null;

    // Cropper.js 인스턴스 저장 변수
    let cropperInstance;

    function showToast(message, callback) {
        const toast = document.getElementById("toast");
        const messageEl = toast.querySelector('.toast-message');
        messageEl.textContent = message;
        toast.classList.add("show");

        setTimeout(() => {
            toast.classList.remove("show");
            if (callback) {
                callback();
            }
        }, 1500);
    }

    // 페이지 로드 시 버튼을 비활성화 상태로 설정
    submitButton.disabled = true;

    // 사용자가 선택한 파일들을 저장할 배열
    let selectedImageFiles = [];

    // '이미지 추가' 버튼 클릭 시, 숨겨진 파일 input을 클릭합니다.
    addImageButton.addEventListener('click', () => {
        imageInput.click();
    });

    // 파일 선택 시 Cropper.js 인스턴스 생성
    imageInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (file) {
            originalImageFile = file;
            const reader = new FileReader();
            reader.onload = (e) => {
                imageToCrop.src = e.target.result;
                imageEditorOverlay.classList.add('show');

                // 기존의 cropper 인스턴스가 있으면 제거
                if (cropperInstance) {
                    cropperInstance.destroy();
                }

                // Cropper.js 인스턴스 생성
                cropperInstance = new Cropper(imageToCrop, {
                    // 3:4 비율로 고정 (세로:가로)
                    aspectRatio: 3 / 4,
                    viewMode: 1, // 크롭 박스가 컨테이너를 벗어나지 않도록 설정
                    dragMode: 'move', // 마우스로 이미지를 이동시킬 수 있도록 설정
                    autoCropArea: 1, // 전체 이미지를 초기 크롭 영역으로 설정
                    ready() {
                        // 이미지 로드 후 축소/이동이 가능하도록 설정
                        this.cropper.zoomTo(1);
                    }
                });
            };
            reader.readAsDataURL(file);
        }
    });

    // '다음' 버튼 클릭 시 크롭된 이미지 가져오기
    editorDoneBtn.addEventListener('click', () => {
        if (!cropperInstance) return;

        // 크롭된 이미지를 Blob 형태로 가져오기
        cropperInstance.getCroppedCanvas().toBlob((blob) => {
            editedImageFile = new File([blob], `cropped_image_${Date.now()}.png`, {type: 'image/png'});

            imagePreviewContainer.innerHTML = '';
            selectedImageFiles = [editedImageFile];
            displayImagePreview(editedImageFile);

            imageEditorOverlay.classList.remove('show');
            imageInput.value = '';
            cropperInstance.destroy(); // 크롭 인스턴스 제거
            cropperInstance = null;
        }, 'image/png', 0.9);
    });

    // 팝업 닫기 버튼에 크롭 인스턴스 제거 로직
    editorCloseBtn.addEventListener('click', () => {
        imageEditorOverlay.classList.remove('show');
        imageInput.value = '';
        if (cropperInstance) {
            cropperInstance.destroy();
            cropperInstance = null;
        }
    });

    // 미리보기 이미지를 생성하는 함수로 분리
    function displayImagePreview(file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            const previewWrapper = document.createElement('div');
            previewWrapper.classList.add('image-preview-wrapper');

            const img = document.createElement('img');
            img.src = e.target.result;
            img.classList.add('img-preview');

            const deleteButton = document.createElement('div');
            deleteButton.classList.add('delete-button');
            deleteButton.innerHTML = 'X';
            deleteButton.addEventListener('click', () => {
                selectedImageFiles.splice(0, 1);
                previewWrapper.remove();
                if (selectedImageFiles.length === 0) {
                    addImageButton.classList.remove('active');
                    imagePreviewContainer.classList.remove('has-image');
                }
            });

            previewWrapper.appendChild(img);
            previewWrapper.appendChild(deleteButton);
            imagePreviewContainer.appendChild(previewWrapper);
        };
        reader.readAsDataURL(file);

        // 이미지 아이콘 색상 및 배경 표시
        addImageButton.classList.add('active');
        imagePreviewContainer.classList.add('has-image');
    }

    // 텍스트 입력에 따라 버튼 활성화/비활성화
    contentInput.addEventListener('input', () => {
        // 입력 필드의 공백을 제거한 길이가 0보다 크면
        if (contentInput.value.trim().length > 0) {
            submitButton.disabled = false; // 버튼 활성화
        } else {
            submitButton.disabled = true;  // 그렇지 않으면 버튼 비활성화
        }
    });

    // 폼 제출 이벤트 처리 ('공유' 버튼 클릭 시)
    submitButton.addEventListener('click', async (e) => {
        e.preventDefault();

        // 텍스트 유효성 검사 로직
        const content = document.getElementById("content").value.trim();
        if (!content) {
            showToast('게시글 내용을 입력해주세요.');
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = '업로드 중...';

        const currentAreaKey = localStorage.getItem('currentAreaKey');
        if (!currentAreaKey) {
            showToast('지역 설정이 필요합니다.');
            submitButton.disabled = false;
            submitButton.textContent = '공유';
            return;
        }

        let imageUrls = [];
        try {
            if (selectedImageFiles.length > 0) {
                const formData = new FormData();
                selectedImageFiles.forEach(file => {
                    formData.append("images", file);
                });

                // 1단계: S3에 이미지 업로드 및 URL 목록 받기
                const imageUploadResponse = await fetch("/images/upload", {
                    method: "POST",
                    body: formData,
                });

                if (!imageUploadResponse.ok) {
                    const errorData = await imageUploadResponse.json();
                    throw new Error(errorData.message || "이미지 업로드에 실패했습니다.");
                }

                const responseData = await imageUploadResponse.json();
                imageUrls = responseData.data;
            }
        } catch (error) {
            console.error('이미지 업로드 오류:', error);
            showToast(`이미지 업로드에 실패했습니다. (${error.message})`);
            submitButton.disabled = false;
            submitButton.textContent = '공유';
            return;
        }

        // 2단계: 게시글 데이터와 이미지 URL 목록을 함께 전송
        const createForumData = {
            content: document.getElementById("content").value,
            location: currentAreaKey,
            imageUrls: imageUrls,
            userEmail: "test@gmail.com",
        };

        try {
            const forumCreateResponse = await fetch("/forum", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(createForumData),
            });

            if (!forumCreateResponse.ok) {
                const errorData = await forumCreateResponse.json();
                throw new Error(errorData.message || "게시글 생성에 실패했습니다.");
            }

            window.location.href = '/forumMain?message=write-success';
        } catch (error) {
            console.error('게시글 생성 오류:', error);
            showToast(`오류가 발생했습니다. (${error.message}) 다시 시도해주세요.`);
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = '공유';
        }
    });
}