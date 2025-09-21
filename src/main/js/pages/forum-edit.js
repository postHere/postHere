export function initForumEdit() {
    const forumId = $('#forum-id').val();
    const forumForm = $('#edit-forum-form');
    const contentTextarea = $('#content');
    const submitButton = $('#submit-btn');
    const imagePreviewContainer = $('#image-preview-container');

    const originalContent = contentTextarea.val();
    const originalImageCount = imagePreviewContainer.find('.image-preview-wrapper').length;
    let musicIsDeleted = false;
    const hasOriginalMusic = $('#music-url-input').length > 0;

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

    const deletedImageIds = [];

    if (originalImageCount > 0) {
        imagePreviewContainer.addClass('has-image');
    }

    const updateSubmitButtonState = () => {
        const contentChanged = contentTextarea.val() !== originalContent;
        const imageCountChanged = imagePreviewContainer.find('.image-preview-wrapper').length !== originalImageCount;
        const musicChanged = hasOriginalMusic && musicIsDeleted;

        const hasChanges = contentChanged || imageCountChanged || musicChanged;

        if (hasChanges) {
            submitButton.prop('disabled', false);
        } else {
            submitButton.prop('disabled', true);
        }
    };

    $('#back-btn').on('click', function () {
        const contentChanged = contentTextarea.val() !== originalContent;
        const imageCountChanged = imagePreviewContainer.find('.image-preview-wrapper').length !== originalImageCount;
        const musicChanged = hasOriginalMusic && musicIsDeleted;
        const hasChanges = contentChanged || imageCountChanged || musicChanged;

        if (!hasChanges) {
            window.location.href = '/forumMain';
        } else {
            history.back();
        }
    });

    contentTextarea.on('input', updateSubmitButtonState);

    imagePreviewContainer.on('click', '.delete-button', function () {
        const button = $(this);
        const mediaType = button.data('type');

        if (mediaType === 'image') {
            const imageId = button.data('image-id');
            if (imageId) {
                deletedImageIds.push(imageId);
                button.closest('.image-preview-wrapper').remove();
                console.log(`이미지 ID ${imageId} 삭제 예정 (배열에 추가됨)`);
            }
        } else if (mediaType === 'music') {
            button.closest('.music-preview-wrapper').remove();
            musicIsDeleted = true;
            console.log('음악 삭제');
        }

        updateSubmitButtonState();

        if (imagePreviewContainer.find('.image-preview-wrapper').length === 0) {
            imagePreviewContainer.removeClass('has-image');
        }
    });

    updateSubmitButtonState();

    forumForm.on('submit', async function (e) {
        e.preventDefault();

        const updatedContents = contentTextarea.val().trim();
        const updatedMusicUrl = $('#music-url-input').val() || null;

        const requestBody = {
            content: updatedContents,
            musicApiUrl: updatedMusicUrl,
            deletedImageIds: deletedImageIds
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
                // 수정 성공 시, 쿼리 파라미터를 추가하여 메인 페이지로 이동
                window.location.href = `/forumMain?message=edit-success`;
            } else {
                showToast('게시글 수정에 실패했습니다: ' + result.message);
            }
        } catch (error) {
            console.error('게시글 수정 오류:', error);
            showToast('게시글 수정 중 오류가 발생했습니다.');
        }
    });
}