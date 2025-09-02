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
        // TODO: 음악 검색 모달을 띄우는 기능 구현 필요
        const trackId = prompt("연결할 Spotify 트랙 ID를 입력하세요:", "4uPiFjZpAfggB4aW2v2p4M");
        if (trackId) {
            document.getElementById('spotifyTrackId').value = trackId;
            alert(`음악이 추가되었습니다: ${trackId}`);
        }
    });

    // 파일 input의 내용이 변경(파일이 선택)되면, 이미지 미리보기를 생성합니다.
    imageInput.addEventListener('change', () => {
        // 기존 미리보기 삭제
        imagePreviewContainer.innerHTML = '';

        const files = imageInput.files;
        if (files.length > 0) {
            // 여러 파일을 순회하며 미리보기 이미지 생성
            Array.from(files).forEach(file => {
                const reader = new FileReader();
                reader.onload = (e) => {
                    const img = document.createElement('img');
                    img.src = e.target.result;
                    img.classList.add('img-preview'); // 여기서 클래스를 추가
                    imagePreviewContainer.appendChild(img);
                };
                reader.readAsDataURL(file); // 파일을 읽어 데이터 URL로 변환
            });
        }
    });

    // 폼 제출 이벤트 처리 (헤더의 '공유' 버튼 또는 폼 제출 시)
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        // 폼 데이터를 담는 FormData 객체
        const formData = new FormData(form);

        // '공유' 버튼 비활성화 (중복 제출 방지)
        submitButton.disabled = true;
        submitButton.textContent = '업로드 중...';

        try {
            // fetch API로 서버에 데이터 전송
            // 이 요청은 브라우저가 로그인 세션 쿠키를 자동으로 포함합니다.
            const response = await fetch('/forum', {
                method: 'POST',
                body: formData,
            });

            const result = await response.json();
            console.log(result);

            if (response.ok && result.status === '000') {
                alert(result.message);
                window.location.href = '/';
            } else {
                alert('작성 실패: ' + (result.message || '서버 오류'));
            }
        } catch (error) {
            console.error('폼 제출 중 오류 발생:', error);
            alert('오류가 발생했습니다. 다시 시도해주세요.');
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = '공유';
        }
    });
});