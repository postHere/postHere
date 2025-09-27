import {Preferences} from '@capacitor/preferences';

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

    // 최대 이미지 및 텍스트 개수 정의
    const MAX_IMAGES = 10;
    const MAX_TEXT_LENGTH = 3000;
    const textCountDisplay = document.getElementById('text-count-display');

    // Cropper.js 인스턴스 저장 변수
    let cropperInstance;

    // 최종적으로 서버에 전송될 편집 완료된 이미지 파일 배열
    let finalImageFiles = [];
    // 사용자가 새로 선택한, 아직 크롭을 거치지 않은 원본 파일 배열 (크롭 대기열)
    let imagesToCropQueue = [];
    // 현재 크롭 중인 파일
    let currentFileBeingCropped = null;


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

    // 텍스트 카운터를 업데이트하는 함수
    const updateTextCounter = () => {
        if (!textCountDisplay) return;

        const currentLength = contentInput.value.length;
        textCountDisplay.textContent = `${currentLength}/${MAX_TEXT_LENGTH}`;

        if (currentLength > MAX_TEXT_LENGTH) {
            textCountDisplay.style.color = 'red';
            // 버튼 비활성화는 'input' 리스너에서 처리
        } else {
            textCountDisplay.style.color = '#333';
        }
    };

    // 이미지 버튼 활성화 상태 업데이트 함수
    const updateImageButtonState = () => {
        const currentCount = finalImageFiles.length;

        // 이미지 추가 버튼 활성화/비활성화 제어
        if (currentCount >= MAX_IMAGES) {
            addImageButton.disabled = true;
            addImageButton.classList.remove('active');
        } else {
            addImageButton.disabled = false;
        }
    };

    // 페이지 로드 시 버튼을 비활성화 상태로 설정
    submitButton.disabled = true;
    updateTextCounter(); // 초기 카운터 표시
    updateImageButtonState(); // 초기 이미지 버튼 상태 설정

    // '이미지 추가' 버튼 클릭 시, 숨겨진 파일 input을 클릭
    addImageButton.addEventListener('click', () => {
        if (finalImageFiles.length >= MAX_IMAGES) {
            showToast(`이미지는 최대 ${MAX_IMAGES}장까지만 업로드할 수 있습니다.`);
            return;
        }
        imageInput.click();
    });

    // 파일 선택 시: 크롭 대기열에 추가하고 첫 번째 크롭 시작
    imageInput.addEventListener('change', (e) => {
        let newFiles = Array.from(e.target.files);
        let spaceLeft = MAX_IMAGES - finalImageFiles.length; // 현재 남은 공간

        // 10장 초과 파일 자르기 및 경고
        if (newFiles.length > spaceLeft) {
            newFiles = newFiles.slice(0, spaceLeft);
            showToast(`최대 ${MAX_IMAGES}장까지만 가능하여 ${spaceLeft}장만 추가됩니다.`);
        }

        // 새 파일들을 크롭 대기열에 추가
        imagesToCropQueue.push(...newFiles);

        // 이미지 입력 필드 초기화 (동일 파일 재선택 가능하게)
        e.target.value = '';

        // 크롭 대기열에 이미지가 있고, 현재 크롭 중인 파일이 없으면 크롭 시작
        if (imagesToCropQueue.length > 0 && !currentFileBeingCropped) {
            processNextImageInQueue();
        }
    });

    // 크롭 대기열에서 다음 이미지를 가져와 편집기를 엽니다.
    function processNextImageInQueue() {
        if (imagesToCropQueue.length === 0) {
            currentFileBeingCropped = null;
            redrawAllPreviews();
            imageEditorOverlay.classList.remove('show'); // 마지막에 팝업 닫기
            return;
        }

        // 큐에서 다음 파일을 꺼냅니다.
        currentFileBeingCropped = imagesToCropQueue.shift();

        const reader = new FileReader();
        reader.onload = (e) => {
            imageToCrop.src = e.target.result;

            // 편집기 팝업 표시
            imageEditorOverlay.classList.add('show');
            if (cropperInstance) {
                cropperInstance.destroy();
            }
            // Cropper.js 인스턴스 생성
            cropperInstance = new Cropper(imageToCrop, {
                aspectRatio: 3 / 4, // 3:4 비율 고정
                viewMode: 1,
                dragMode: 'move',
                autoCropArea: 1,
                ready() {
                    this.cropper.zoomTo(1);
                }
            });
        };
        reader.readAsDataURL(currentFileBeingCropped);
    }

    // '다음' 버튼 클릭 시 크롭된 이미지 가져오기
    editorDoneBtn.addEventListener('click', () => {
        if (!cropperInstance || !currentFileBeingCropped) return;

        // 크롭된 이미지를 Blob 형태로 가져오기
        cropperInstance.getCroppedCanvas().toBlob((blob) => {
            // 크롭된 새 파일 객체 생성 및 최종 배열에 추가
            const editedFile = new File([blob], `cropped_image_${Date.now()}.jpeg`, {type: 'image/jpeg'});
            finalImageFiles.push(editedFile);

            // 1. 팝업의 이미지를 크롭된 결과물로 교체 (렌더링 피드백)
            const reader = new FileReader();

            reader.onload = (e) => {
                imageToCrop.src = e.target.result;

                // 2. Cropper 인스턴스 파괴
                if (cropperInstance) {
                    cropperInstance.destroy();
                }
                currentFileBeingCropped = null; // 정리

                // 3. DOM 강제 리플로우 (잔상 제거의 핵심!)
                void imageEditorOverlay.offsetHeight;

                // 4. 팝업을 즉시 닫고 다음 로직을 실행
                imageEditorOverlay.classList.remove('show');
                processNextImageInQueue();
            };
            reader.readAsDataURL(blob); // 비동기 로딩 시작

        }, 'image/jpeg', 0.9);
    });

    // 팝업 닫기 버튼에 크롭 인스턴스 제거 로직 (편집 취소)
    editorCloseBtn.addEventListener('click', () => {
        // 현재 크롭 중이던 파일을 최종 배열에 추가하지 않고 버림

        // 크롭 세션 종료
        if (cropperInstance) {
            cropperInstance.destroy();
            cropperInstance = null;
        }
        currentFileBeingCropped = null;

        imageEditorOverlay.classList.remove('show');

        // 다음 이미지 크롭 시작
        processNextImageInQueue();
    });

    // finalImageFiles 배열의 모든 파일을 기반으로 미리보기를 다시 그림
    function redrawAllPreviews() {
        imagePreviewContainer.innerHTML = ''; // 기존 미리보기 모두 제거
        finalImageFiles.forEach(file => displaySingleImagePreview(file));

        if (finalImageFiles.length === 0) {
            addImageButton.classList.remove('active');
            imagePreviewContainer.classList.remove('has-image');
        } else {
            addImageButton.classList.add('active');
            imagePreviewContainer.classList.add('has-image');
        }

        // 이미지 버튼 상태 업데이트 호출
        updateImageButtonState();

    }

    // 미리보기 이미지를 생성하고 삭제 버튼을 연결하는 함수
    function displaySingleImagePreview(file) {
        const reader = new FileReader();
        reader.onload = (e) => {
            const previewWrapper = document.createElement('div');
            previewWrapper.classList.add('image-preview-wrapper');

            const img = document.createElement('img');
            img.src = e.target.result;
            img.classList.add('img-preview');

            // 미리보기 클릭 시 재편집 기능 (옵션)
            img.addEventListener('click', () => {
            });

            const deleteButton = document.createElement('div');
            deleteButton.classList.add('delete-button');
            deleteButton.innerHTML = 'X';

            // 삭제 버튼 클릭 시 해당 파일을 finalImageFiles 배열에서 찾아 삭제
            deleteButton.addEventListener('click', () => {
                const fileToRemove = file;

                const indexToRemove = finalImageFiles.findIndex(f => f === fileToRemove);

                if (indexToRemove !== -1) {
                    finalImageFiles.splice(indexToRemove, 1);
                }

                // 삭제 후 미리보기를 다시 그림
                redrawAllPreviews();
            });

            previewWrapper.appendChild(img);
            previewWrapper.appendChild(deleteButton);
            imagePreviewContainer.appendChild(previewWrapper);
        };
        reader.readAsDataURL(file);
    }

    // 텍스트 입력에 따라 버튼 활성화/비활성화 (게시글 등록의 관건은 텍스트만!)
    contentInput.addEventListener('input', () => {

        // 텍스트 카운터 및 유효성 검사 수행
        updateTextCounter();

        const currentLength = contentInput.value.length;
        const isTextValid = currentLength > 0 && currentLength <= MAX_TEXT_LENGTH;

        // 텍스트가 유효할 때만 버튼 활성화
        if (isTextValid) {
            submitButton.disabled = false; // 버튼 활성화
        } else {
            submitButton.disabled = true;  // 그렇지 않으면 버튼 비활성화
        }
    });

    // 폼 제출 이벤트 처리 ('공유' 버튼 클릭 시)
    submitButton.addEventListener('click', async (e) => {
        e.preventDefault();

        // 텍스트 유효성 재확인 (submit 직전에 최종 검사)
        const content = document.getElementById("content").value.trim();
        if (!content || content.length > MAX_TEXT_LENGTH) {
            showToast('게시글 내용을 3000자 이내로 입력해주세요.');
            submitButton.disabled = false;
            submitButton.textContent = '공유';
            return;
        }

        // 크롭이 진행 중이면 제출을 막습니다.
        if (imagesToCropQueue.length > 0 || currentFileBeingCropped) {
            showToast('이미지 편집을 완료하거나 취소해주세요.');
            submitButton.disabled = false;
            submitButton.textContent = '공유';
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = '업로드 중...';

        const {value: postingAreaKey} = await Preferences.get({key: 'viewingAreaKey'});
        const {value: actualAreaKey} = await Preferences.get({key: 'currentAreaKey'});

        // 지역 키가 유효하지 않으면 작성 차단
        if (!postingAreaKey || postingAreaKey !== actualAreaKey) {
            showToast('작성 가능한 지역이 아닙니다.');
            submitButton.disabled = false;
            submitButton.textContent = '공유';
            return;
        }

        let imageUrls = [];
        try {
            if (finalImageFiles.length > 0) {
                const formData = new FormData();
                finalImageFiles.forEach(file => {
                    formData.append("images", file);
                });

                // 1단계: S3에 이미지 업로드 및 URL 목록 받기
                const imageUploadResponse = await fetch("/images/upload", {
                    method: "POST",
                    body: formData,
                });

                if (!imageUploadResponse.ok) {
                    const contentType = imageUploadResponse.headers.get("content-type");
                    let errorMessage = "이미지 업로드에 실패했습니다.";
                    if (contentType && contentType.includes("application/json")) {
                        const errorData = await imageUploadResponse.json();
                        errorMessage = errorData.message || errorMessage;
                    }
                    throw new Error(errorMessage);
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
            location: postingAreaKey,
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