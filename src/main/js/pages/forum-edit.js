export function initForumEdit() {
    const forumId = $('#forum-id').val();
    const forumForm = $('#edit-forum-form');
    const contentTextarea = $('#forum-content');
    const musicUrlInput = $('#music-url-input');

    // 삭제된 이미지 ID를 담을 배열
    const deletedImageIds = [];

    // 뒤로가기 버튼 이벤트
    $('#back-button').on('click', function () {
        window.location.href = '/forumMain';
    });

    // 사진/음악 삭제 버튼 이벤트
    $('.delete-media-button').on('click', function () {
        const button = $(this);
        const mediaType = button.data('type');

        if (mediaType === 'image') {
            const imageId = button.data('image-id');
            if (imageId) {
                // 서버에 요청을 보내지 않고 ID만 배열에 추가
                deletedImageIds.push(imageId);
                // UI에서만 해당 이미지 요소를 숨김
                button.closest('.media-item').hide();
                console.log(`이미지 ID ${imageId} 삭제 예정 (배열에 추가됨)`);
            }
        } else if (mediaType === 'music') {
            button.closest('.music-item').remove();
            musicUrlInput.val('');
            console.log('음악 삭제');
        }
    });

    // 폼 제출 (수정 완료) 이벤트
    forumForm.on('submit', async function (e) {
        e.preventDefault();

        const updatedContents = contentTextarea.val().trim();
        const updatedMusicUrl = musicUrlInput.val() || null;

        const requestBody = {
            content: updatedContents,
            musicApiUrl: updatedMusicUrl,
            deletedImageIds: deletedImageIds // ★★★ 변경: 삭제할 ID 목록을 직접 전송 ★★★
        };

        try {
            const response = await fetch(`/forum/${forumId}`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            if (result.status === '000') {
                alert('게시글이 성공적으로 수정되었습니다.');
                window.location.href = result.data;
            } else {
                alert('게시글 수정에 실패했습니다: ' + result.message);
            }
        } catch (error) {
            console.error('게시글 수정 오류:', error);
            alert('게시글 수정 중 오류가 발생했습니다.');
        }
    });
}