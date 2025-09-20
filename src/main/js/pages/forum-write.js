export function initForumWrite() {

    const submitButton = document.getElementById('submit-btn');
    const imageInput = document.getElementById('images');
    const addImageButton = document.getElementById('add-image-btn');
    const imagePreviewContainer = document.getElementById('image-preview-container');
    const contentInput = document.getElementById('content');

    // 페이지 로드 시 버튼을 비활성화 상태로 설정
    submitButton.disabled = true;

    // 사용자가 선택한 파일들을 저장할 배열
    let selectedImageFiles = [];

    // '이미지 추가' 버튼 클릭 시, 숨겨진 파일 input을 클릭합니다.
    addImageButton.addEventListener('click', () => {
        imageInput.click();
    });

    // 파일 input의 내용이 변경(파일이 선택)되면, 미리보기만 생성합니다.
    imageInput.addEventListener('change', () => {
        imagePreviewContainer.innerHTML = '';
        selectedImageFiles = [];

        const files = imageInput.files;
        if (files.length > 0) {
            selectedImageFiles = Array.from(files);

            // 이미지 아이콘 색상 변경
            addImageButton.classList.add('active');

            // 이미지가 삽입되면 배경 표시
            imagePreviewContainer.classList.add('has-image');

            selectedImageFiles.forEach(file => {
                const reader = new FileReader();
                reader.onload = (e) => {
                    // 미리보기 컨테이너와 삭제 버튼을 함께 생성
                    const previewWrapper = document.createElement('div');
                    previewWrapper.classList.add('image-preview-wrapper');

                    const img = document.createElement('img');
                    img.src = e.target.result;
                    img.classList.add('img-preview');

                    const deleteButton = document.createElement('div');
                    deleteButton.classList.add('delete-button');
                    deleteButton.innerHTML = 'X';
                    // 삭제 버튼 클릭 시 이벤트 리스너 추가
                    deleteButton.addEventListener('click', () => {
                        // 선택된 이미지 배열에서 해당 이미지 삭제
                        selectedImageFiles.splice(index, 1);
                        previewWrapper.remove();
                        // 삭제 후 이미지가 없으면 아이콘 색상 원래대로
                        if (selectedImageFiles.length === 0) {
                            addImageButton.classList.remove('active');
                            // 이미지가 없으면 배경을 숨기도록 클래스 제거
                            imagePreviewContainer.classList.remove('has-image');
                        }
                    });

                    previewWrapper.appendChild(img);
                    previewWrapper.appendChild(deleteButton);
                    imagePreviewContainer.appendChild(previewWrapper);
                };
                reader.readAsDataURL(file);
            });
        } else {
            // 파일 선택이 취소되면 아이콘 색상 원래대로
            addImageButton.classList.remove('active');
            // 이미지가 없으면 배경을 숨기도록 클래스 제거
            imagePreviewContainer.classList.remove('has-image');
        }
    });

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
            alert('게시글 내용을 입력해주세요.');
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = '업로드 중...';

        const currentAreaKey = localStorage.getItem('currentAreaKey');
        if (!currentAreaKey) {
            alert('지역 설정이 필요합니다.');
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
            alert(`이미지 업로드에 실패했습니다. (${error.message})`);
            submitButton.disabled = false;
            submitButton.textContent = '공유';
            return;
        }

        // 2단계: 게시글 데이터와 이미지 URL 목록을 함께 전송
        const createForumData = {
            content: document.getElementById("content").value,
            location: currentAreaKey,
            imageUrls: imageUrls,
            spotifyTrackId: document.getElementById("spotifyTrackId").value,
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

            alert("게시글이 성공적으로 작성되었습니다!");
            window.location.href = '/';
        } catch (error) {
            console.error('게시글 생성 오류:', error);
            alert(`오류가 발생했습니다. (${error.message}) 다시 시도해주세요.`);
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = '공유';
        }
    });

    // 음악 추가 버튼 클릭 시 동작
    document.getElementById('add-music-btn').addEventListener('click', () => {
        const trackId = prompt("연결할 Spotify 트랙 ID를 입력하세요:", "4uPiFjZpAfggB4aW2v2p4M");
        if (trackId) {
            document.getElementById('spotifyTrackId').value = trackId;
            alert(`음악이 추가되었습니다: ${trackId}`);
        }
    });
}