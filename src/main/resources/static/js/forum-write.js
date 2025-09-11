document.addEventListener('DOMContentLoaded', () => {

    const form = document.getElementById('forum-form');
    const submitButton = document.getElementById('submit-btn');
    const imageInput = document.getElementById('images');
    const addImageButton = document.getElementById('add-image-btn');
    const imagePreviewContainer = document.getElementById('image-preview-container');

    // '이미지 추가(🖼️)' 버튼 클릭 시, 숨겨진 파일 input을 클릭합니다.
    addImageButton.addEventListener('click', () => {
        imageInput.click();
    });

    // '음악 추가(🎵)' 버튼 클릭 시 동작 (지금은 알림창만 띄웁니다)
    document.getElementById('add-music-btn').addEventListener('click', () => {
        const trackId = prompt("연결할 Spotify 트랙 ID를 입력하세요:", "4uPiFjZpAfggB4aW2v2p4M");
        if (trackId) {
            document.getElementById('spotifyTrackId').value = trackId;
            alert(`음악이 추가되었습니다: ${trackId}`);
        }
    });

    // 파일 input의 내용이 변경(파일이 선택)되면, 이미지 미리보기를 생성합니다.
    imageInput.addEventListener('change', () => {
        imagePreviewContainer.innerHTML = '';
        const files = imageInput.files;
        if (files.length > 0) {
            Array.from(files).forEach(file => {
                const reader = new FileReader();
                reader.onload = (e) => {
                    const img = document.createElement('img');
                    img.src = e.target.result;
                    img.classList.add('img-preview');
                    imagePreviewContainer.appendChild(img);
                };
                reader.readAsDataURL(file);
            });
        }
    });

    // 폼 제출 이벤트 처리 (헤더의 '공유' 버튼 또는 폼 제출 시)
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        // '공유' 버튼 비활성화 (중복 제출 방지)
        submitButton.disabled = true;
        submitButton.textContent = '업로드 중...';

        let imageUrls = [];
        try {
            const imageFiles = imageInput.files;
            if (imageFiles.length > 0) {
                const formData = new FormData();
                for (let i = 0; i < imageFiles.length; i++) {
                    formData.append("images", imageFiles[i]);
                }
                const imageUploadResponse = await fetch("/images/upload", {
                    method: "POST",
                    body: formData,
                });
                if (!imageUploadResponse.ok) {
                    throw new Error("이미지 업로드에 실패했습니다.");
                }
                const imageData = await imageUploadResponse.json();
                imageUrls = imageData.data;
            }
        } catch (error) {
            console.error('이미지 업로드 오류:', error);
            alert('이미지 업로드에 실패했습니다. 다시 시도해주세요.');
            submitButton.disabled = false;
            submitButton.textContent = '공유';
            return;
        }

        // 2단계: 이미지 URL을 포함한 게시글 데이터를 JSON으로 서버에 보냅니다.
        const createForumData = {
            writerId: document.getElementById("writerId").value,
            content: document.getElementById("content").value,
            // location 값을 localStorage에서 가져와 설정
            location: localStorage.getItem('currentAreaKey'),
            imageUrls: imageUrls,
            spotifyTrackId: document.getElementById("spotifyTrackId").value,
            userEmail: "test@gmail.com",
        };

        try {
            const forumCreateResponse = await fetch("/forum", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json", // Content-Type을 application/json으로 명시
                },
                body: JSON.stringify(createForumData), // JSON.stringify로 데이터를 변환
            });

            if (!forumCreateResponse.ok) {
                throw new Error("게시글 생성에 실패했습니다.");
            }

            const forumData = await forumCreateResponse.json();
            alert("게시글이 성공적으로 작성되었습니다!");
            window.location.href = '/';
        } catch (error) {
            console.error('게시글 생성 오류:', error);
            alert('오류가 발생했습니다. 다시 시도해주세요.');
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = '공유';
        }
    });
});