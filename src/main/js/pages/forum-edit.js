export function initForumEdit() {
    const forumId = $('#forum-id').val();
    const forumForm = $('#edit-forum-form');
    const contentTextarea = $('#content');
    const submitButton = $('#submit-btn');
    const imagePreviewContainer = $('#image-preview-container');

    const MAX_TEXT_LENGTH = 3000;
    const textCountDisplay = document.getElementById('text-count-display');

    const originalContent = contentTextarea.val();
    const originalImageCount = imagePreviewContainer.find('.image-preview-wrapper').length;

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

    // 텍스트 카운터 업데이트 함수
    const updateTextCounter = () => {
        const currentLength = contentTextarea.val().length;
        textCountDisplay.textContent = `${currentLength}/${MAX_TEXT_LENGTH}`;

        if (currentLength > MAX_TEXT_LENGTH) {
            textCountDisplay.style.color = 'red';
            // 버튼 비활성화는 updateSubmitButtonState가 처리
        } else {
            textCountDisplay.style.color = '#666';
        }
    };

    const updateSubmitButtonState = () => {
        const currentContent = contentTextarea.val();
        const currentLength = currentContent.length;

        // 현재 텍스트 내용이 공백을 제외하고 1자 이상이며, 길이 제한을 넘지 않는지 확인
        const isContentValid = currentContent.trim().length > 0 && currentLength <= MAX_TEXT_LENGTH;

        const contentChanged = contentTextarea.val() !== originalContent;
        const imageCountChanged = imagePreviewContainer.find('.image-preview-wrapper').length !== originalImageCount;

        // 변경 사항이 있으면서 동시에 내용이 유효할 때만 버튼 활성화
        const hasChangesAndValid = (contentChanged || imageCountChanged) && isContentValid;

        if (hasChangesAndValid) {
            submitButton.prop('disabled', false);
        } else {
            submitButton.prop('disabled', true);
        }
    };

    // 페이지 로드 시 카운터 초기화
    updateTextCounter();

    $('#back-btn').on('click', function () {
        const contentChanged = contentTextarea.val() !== originalContent;
        const imageCountChanged = imagePreviewContainer.find('.image-preview-wrapper').length !== originalImageCount;
        const hasChanges = contentChanged || imageCountChanged;

        if (!hasChanges) {
            window.location.href = '/forumMain';
        } else {
            history.back();
        }
    });

    // 입력 시점에 텍스트 길이 제한을 넘는 텍스트가 들어오지 않도록 방지
    contentTextarea.on('input', (e) => {
        const currentLength = contentTextarea.val().length;

        // 1. 길이 초과 시 입력 방지 및 경고
        if (currentLength > MAX_TEXT_LENGTH) {
            const trimmedValue = contentTextarea.val().substring(0, MAX_TEXT_LENGTH);
            contentTextarea.val(trimmedValue);
            showToast(`텍스트는 최대 ${MAX_TEXT_LENGTH}자까지만 입력할 수 있습니다.`);
            return;
        }
        // 2. 카운터 및 버튼 상태 업데이트
        updateTextCounter();
        updateSubmitButtonState();
    });

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
        }

        updateSubmitButtonState();

        if (imagePreviewContainer.find('.image-preview-wrapper').length === 0) {
            imagePreviewContainer.removeClass('has-image');
        }
    });

    updateSubmitButtonState();

    submitButton.on('click', async function (e) {
        e.preventDefault(); // 버튼의 기본 submit 동작을 항상 막습니다.

        const updatedContents = contentTextarea.val().trim();
        const finalLength = updatedContents.length;

        // 유효성 검사 분기 로직 (토스트를 띄우는 역할)
        if (!updatedContents) {
            // 텍스트가 공백이거나 0자일 때 showToast를 띄웁니다.
            showToast('게시글 내용을 입력해주세요.');
            return; // 제출 로직 실행 중단
        }

        if (finalLength > MAX_TEXT_LENGTH) {
            showToast(`게시글 내용을 ${MAX_TEXT_LENGTH}자 이내로 입력해주세요.`);
            return; // 제출 로직 실행 중단
        }

        // 버튼을 다시 비활성화하고 로딩 상태로
        submitButton.prop('disabled', true);
        submitButton.text('업데이트 중...');

        const requestBody = {
            content: updatedContents,
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
                window.location.href = `/forumMain?message=edit-success`;
            } else {
                showToast('게시글 수정에 실패했습니다: ' + result.message);
            }
        } catch (error) {
            console.error('게시글 수정 오류:', error);
            showToast('게시글 수정 중 오류가 발생했습니다.');
        } finally {
            // 오류나 실패 시 버튼 상태 복구
            submitButton.prop('disabled', false);
            submitButton.text('완료');
        }
    });
}