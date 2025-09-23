export function initMain() {
    let finalAreaKey = null;
    const urlParams = new URLSearchParams(window.location.search);
    const areaKeyFromUrl = urlParams.get('areaKey');

    // 로고 클릭 이벤트 리스너
    $('.logo a').on('click', function (e) {
        e.preventDefault(); // 기본 링크 동작을 막음
        // 현재 위치 키를 다시 불러와서 게시글을 로드
        if (finalAreaKey) {
            loadPosts(finalAreaKey);
        } else {
            // 위치가 설정되지 않았다면 기본 동작 (전체 새로고침)
            window.location.reload();
        }
    });

    // URL에서 메시지 파라미터 확인 및 알림 띄우기
    const messageParam = urlParams.get('message');
    if (messageParam === 'edit-success') {
        showToast('게시글이 성공적으로 수정되었습니다.');
        // 알림을 띄운 후, URL에서 파라미터 제거
        const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
        history.replaceState({}, document.title, newUrl);
    } else if (messageParam === 'write-success') {
        showToast('게시글이 성공적으로 작성되었습니다!');
        const newUrl = window.location.protocol + "//" + window.location.host + window.location.pathname;
        history.replaceState({}, document.title, newUrl);
    }

    if (areaKeyFromUrl) {
        finalAreaKey = areaKeyFromUrl;
    } else {
        const areaKeyFromStorage = localStorage.getItem('currentAreaKey');
        if (areaKeyFromStorage) {
            finalAreaKey = areaKeyFromStorage;
        }
    }

    const locationTextElement = $('#current-location-text');
    if (finalAreaKey) {
        locationTextElement.text(finalAreaKey);
        loadPosts(finalAreaKey);
    } else {
        locationTextElement.text("지역 설정 중..");
    }

    function showToast(message) {
        const toast = document.getElementById("toast");
        const messageEl = toast.querySelector('.toast-message');
        messageEl.textContent = message;
        toast.classList.add("show");

        // 초 후에 사라지게
        setTimeout(() => {
            toast.classList.remove("show");
        }, 1500);
    }

    // 댓글 모달을 여는 로직
    $('#post-list-container').on('click', '.comment-trigger', async function (e) {
        e.preventDefault();
        const postCard = $(this).closest('.post-card');
        const postId = postCard.data('post-id');
        openCommentModal(postId, postCard);
    });

    // 모달 내 댓글 입력 폼 제출 핸들러
    $('body').on('submit', '#comment-modal .comment-form', async function (e) {
        e.preventDefault();
        const form = $(this);
        const modal = form.closest('#comment-modal');
        const postId = modal.data('post-id');
        const input = form.find('.comment-input');
        const content = input.val().trim();
        if (!content) return;
        const submitButton = form.find('.comment-submit');
        submitButton.prop('disabled', true);
        try {
            const response = await fetch(`/api/forum/${postId}/comments`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({content})
            });
            /** HTTP 상태 코드 200번대만 정상 처리하는 조건 */
            if (!response.ok) {
                if (response.status === 401) {
                    showToast('로그인이 필요합니다.');
                    window.location.href = '/login';
                    return;
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const newComment = (await response.json()) || {};
            addCommentToModalDOM(newComment, modal);
            input.val('');
            /* 댓글 작성 후 모달 내의 댓글 개수를 업데이트하는 함수 호출 */
            updateCommentCountInModal(modal);
            showToast('댓글이 작성되었습니다.');
        } catch (error) {
            showToast('댓글 작성에 실패했습니다.');
        } finally {
            submitButton.prop('disabled', false);
        }
    });

    $('#post-list-container').on('click', '.like-button', async function () {
        const likeButton = $(this);
        const forumId = likeButton.data('forum-id');
        try {
            const response = await fetch(`/forum/like/${forumId}`, {
                method: 'POST'
            });
            if (!response.ok) {
                if (response.status === 401) {
                    showToast('로그인이 필요합니다.');
                    window.location.href = '/login';
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const result = await response.json();
            if (result.status === '000') {
                updateLikeStatus(likeButton, result.data);
            } else {
                showToast('좋아요 기능에 문제가 발생했습니다.');
            }
        } catch (error) {
            showToast('좋아요 요청 중 오류가 발생했습니다.');
        }
    });

    // 모달창을 '...' 버튼 바로 아래에 생성하는 로직으로 변경
    $('#post-list-container').on('click', '.post-options-button', function (event) {
        event.stopPropagation();
        const button = $(this);
        const postId = button.data('forum-id');
        $('.options-modal').remove();
        const modal = $(`
            <div class="options-modal">
                <button class="edit-button">수정</button>
                <button class="delete-button">삭제</button>
            </div>
        `);
        button.parent().after(modal);
        setTimeout(() => {
            $(document).one('click', function (e) {
                if (!$(e.target).closest('.options-modal, .post-options-button').length) {
                    modal.remove();
                }
            });
        }, 0);

        // 게시물 삭제 버튼 클릭 핸들러 (모달 없이 즉시 삭제)
        modal.on('click', '.delete-button', async function () {
            modal.remove(); // 옵션 모달만 제거
            const response = await fetch(`/forum/${postId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                throw new Error('게시글 삭제에 실패했습니다.');
            }
            showToast('게시글이 성공적으로 삭제되었습니다.');
            $(`.post-card[data-post-id="${postId}"]`).remove();
        });

        modal.on('click', '.edit-button', function () {
            modal.remove();
            window.location.href = `/forum/${postId}/edit`;
        });
    });

    // 댓글 삭제 버튼 클릭 시 바로 삭제
    $('body').on('click', '.comment-delete-button', async function () {
        const button = $(this);
        const commentId = button.data('comment-id');
        const modal = button.closest('#comment-modal');
        const postId = modal.data('post-id');

        try {
            const response = await fetch(`/api/forum/${postId}/comments/${commentId}`, {
                method: 'DELETE'
            });
            if (response.status === 204) {
                $(`.comment-item[data-comment-id="${commentId}"]`).remove();
                updateCommentCountInModal(modal);
                showToast('댓글이 삭제되었습니다.');
            } else if (response.status === 401) {
                showToast('로그인이 필요합니다.');
                window.location.href = '/login';
            } else if (response.status === 403) {
                showToast('댓글 삭제 권한이 없습니다.');
            } else {
                throw new Error('댓글 삭제에 실패했습니다.');
            }
        } catch (error) {
            showToast('삭제 중 오류가 발생했습니다.');
        }
    });

    // 닫기 버튼 공통 핸들러
    $('body').on('click', '.modal .close-button, .modal .custom-confirm-no', function () {
        $(this).closest('.modal').fadeOut(300);
    });

    function createEmptyPostHtml() {
        const mapIconSvg = `
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="1 6 1 22 8 18 16 22 23 18 23 2 16 6 8 2 1 6"></polygon>
                <line x1="16" y1="6" x2="16" y2="22"></line>
                <line x1="8" y1="2" x2="8" y2="18"></line>
            </svg>
        `;
        return `
            <div class="empty-forum-container">
                <div class="empty-icon-wrapper">
                    ${mapIconSvg}
                </div>
                <p class="empty-text">해당 지역에는 작성된 Forum이 없어요.</p>
                <p class="empty-subtext">다른 이야기를 만나고 싶다면,<br>발걸음을 옮기거나 상단 아이콘을 눌러 함께해요.</p>
            </div>
        `;
    }

    // 게시글 데이터를 post-card에 저장
    function loadPosts(key) {
        $.ajax({
            url: `/forum/area/${key}`,
            type: 'GET',
            dataType: 'json',
            success: function (result) {
                const container = $('#post-list-container');
                container.empty();
                if (result.status === '000' && result.data && result.data.length > 0) {
                    result.data.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
                    result.data.forEach(post => {
                        const postHtml = createPostHtml(post);
                        const newPostCard = $(postHtml);
                        newPostCard.data('post-data', post); // 게시글 데이터 저장

                        container.append(newPostCard);
                        initCarousel(newPostCard);
                    });
                } else {
                    container.html(createEmptyPostHtml());
                }
            },
            error: function () {
                container.html(createEmptyPostHtml());
            }
        });
    }

    // 댓글 모달을 여는 함수
    async function openCommentModal(postId, postCard) {
        // 기존 모달이 있다면 제거
        $('#comment-modal').remove();

        const postData = postCard.data('post-data');

        const modalHtml = `
            <div id="comment-modal" class="comment-modal" data-post-id="${postId}">
                <div class="comment-modal-content">
                    <div class="comment-modal-top-bar">
                        <div class="modal-header-left">
                            <img alt="${postData.writerNickname}" src="${postData.writerProfilePhotoUrl}" class="profile-img">
                            <div class="post-author-info">
                                <div class="name">${postData.writerNickname}</div>
                                <div class="time">${calculateTimeAgo(postData.createdAt)}</div>
                            </div>
                        </div>
                         <button class="comment-modal-close">×</button>
                    </div>
                    <div class="comment-modal-body">
                        <ul class="comment-modal-list"></ul>
                    </div>
                    <div class="comment-modal-footer">
                        <div class="comment-form-inner">
                            <img src="${postData.writerProfilePhotoUrl}" alt="내 프로필" class="profile-img">
                            <form class="comment-form">
                                <input class="comment-input" placeholder="댓글을 작성하세요." required type="text">
                                <button class="comment-submit" type="submit">게시</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        `;
        $('body').append(modalHtml);

        const modal = $('#comment-modal');
        modal.data('post-card', postCard);

        setTimeout(() => {
            modal.addClass('show');
        }, 10);

        modal.on('click', '.comment-modal-close', function () {
            closeCommentModal();
        });

        $(document).on('click', function (e) {
            if (!$(e.target).closest('.comment-modal-content, .comment-trigger').length) {
                closeCommentModal();
            }
        });

        await loadCommentsForModal(postId, modal);
    }

    // 모달용 댓글을 DOM에 추가하는 함수
    function addCommentToModalDOM(comment, modal) {
        const commentList = modal.find('.comment-modal-list');

        const isMyComment = comment.isMyComment; // 백엔드 응답에 이 속성이 있다고 가정
        const deleteButtonHtml = isMyComment ? `<button class="comment-delete-button" data-comment-id="${comment.id}">×</button>` : '';

        const itemHtml = `
            <li class="comment-item" data-comment-id="${comment.id}">
                <img src="${comment.authorProfileImageUrl}" alt="${comment.authorNickname}" class="profile-img">
                <div class="comment-detail-info">
                    <div class="comment-header">
                        <span class="author">${comment.authorNickname}</span>
                        <span class="time">${calculateTimeAgo(comment.createdAt)}</span>
                        ${deleteButtonHtml}
                    </div>
                    <p class="comment-content-text">${escapeHTML(comment.content)}</p>
                </div>
            </li>
        `;
        commentList.append(itemHtml);
    }

    // 댓글 모달을 닫는 함수
    function closeCommentModal() {
        $('#comment-modal').remove();
        $(document).off('click', closeCommentModal); // 이벤트 핸들러 제거
    }

    /// 모달용 댓글 목록을 불러와서 표시하는 함수
    async function loadCommentsForModal(postId, modal) {
        try {
            const response = await fetch(`/api/forum/${postId}/comments`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const comments = await response.json();
            const commentList = modal.find('.comment-modal-list');
            commentList.empty(); // 기존 댓글 비우기

            // 댓글 데이터를 순회하며 DOM에 추가
            if (comments && comments.length > 0) {
                comments.forEach(comment => addCommentToModalDOM(comment, modal));
            }
        } catch (error) {
            console.error('Failed to load comments:', error);
            showToast('댓글을 불러오는 데 실패했습니다.');
        }
    }

    // 모달 내 댓글 수 업데이트 함수를 새로 만든다
    function updateCommentCountInModal(modal) {
        const commentCount = modal.find('.comment-modal-list').children().length;
        const postCard = modal.data('post-card');
        if (postCard) {
            const commentCountSpan = postCard.find('.comment-count');
            commentCountSpan.text(commentCount);
        }
    }

    function initCarousel(postCard) {
        const container = postCard.find('.carousel-images-container');
        const images = postCard.find('.post-image');
        const prevBtn = postCard.find('.carousel-prev-btn');
        const nextBtn = postCard.find('.carousel-next-btn');
        const indicators = postCard.find('.carousel-indicators .indicator');
        let currentIndex = 0;
        const totalImages = images.length;
        if (totalImages <= 1) {
            prevBtn.hide();
            nextBtn.hide();
            indicators.parent().hide();
            return;
        }

        function updateCarousel() {
            const offset = -currentIndex * 100;
            container.css('transform', `translateX(${offset}%)`);
            indicators.removeClass('active');
            $(indicators[currentIndex]).addClass('active');
        }

        prevBtn.on('click', () => {
            currentIndex = (currentIndex > 0) ? currentIndex - 1 : totalImages - 1;
            updateCarousel();
        });
        nextBtn.on('click', () => {
            currentIndex = (currentIndex < totalImages - 1) ? currentIndex + 1 : 0;
            updateCarousel();
        });
        indicators.on('click', function () {
            currentIndex = $(this).index();
            updateCarousel();
        });
        updateCarousel();
    }

    // 하트 아이콘을 유니코드 문자로 직접 사용
    function createPostHtml(post) {
        const imagesHtml = post.imageUrls && post.imageUrls.length > 0 ? `
            <div class="post-image-carousel">
                <div class="carousel-images-container">
                    ${post.imageUrls.map(url => `<img alt="게시물 사진" class="post-image" src="${url}">`).join('')}
                </div>
                <div class="carousel-indicators">
                    ${post.imageUrls.map((_, index) => `<span class="indicator ${index === 0 ? 'active' : ''}"></span>`).join('')}
                </div>
                ${post.imageUrls.length > 1 ? `<button class="carousel-prev-btn prev-icon"></button>
                <button class="carousel-next-btn next-icon"></button>` : ''}
            </div>
        ` : '';
        const timeAgoText = calculateTimeAgo(post.createdAt);
        const recentLikerPhotosHtml = (post.recentLikerPhotos && post.recentLikerPhotos.length > 0) ?
            post.recentLikerPhotos.map(photo => `<img src="${photo}" class="liker-profile-img">`).join('')
            : '';
        const optionsHtml = post.author ? `
            <div class="post-options-container">
                <button class="post-options-button" data-forum-id="${post.id}">...</button>
            </div>
        ` : '';
        const likeButtonClass = post.isLiked ? 'like-button liked' : 'like-button';
        const heartIcon = post.isLiked ? '♥' : '♡';
        return `
           <div class="post-card" data-post-id="${post.id}">
                <div class="post-header">
                    <div class="post-author">
                        <a href="/profile/${post.writerNickname}" class="profile-link">
                            <img alt="${post.writerNickname}" src="${post.writerProfilePhotoUrl}" class="profile-img">
                        </a>
                        <div class="post-author-info">
                             <a href="/profile/${post.writerNickname}" class="profile-link name">${post.writerNickname}</a>
                            <div class="time">${timeAgoText}</div>
                        </div>
                    </div>
                    ${optionsHtml}
                </div>
                <div class="post-content">
                    <p>${escapeHTML(post.contentsText)}</p>
                    ${imagesHtml}
                </div>
                <div class="post-actions">
                    <div>
                        <span class="${likeButtonClass}" data-forum-id="${post.id}">
                            <span class="like-icon">${heartIcon}</span>
                            <span class="like-count">${post.totalLikes}</span> likes
                        </span>
                        <a class="comment-trigger" href="#">💬 <span class="comment-count">${post.totalComments}</span> comments</a>
                    </div>
                </div>
                <div class="liker-photos">
                    ${recentLikerPhotosHtml}
                </div>
            </div>
        `;
    }

    // 하트 색상을 유니코드로 변경
    function updateLikeStatus(likeButton, data) {
        const likeIcon = likeButton.find('.like-icon');
        const likeCount = likeButton.find('.like-count');
        const postCard = likeButton.closest('.post-card');
        const likerPhotosContainer = postCard.find('.liker-photos');

        likeButton.toggleClass('liked', data.liked);
        likeIcon.text(data.liked ? '♥' : '♡'); // 변경: 하트 문자 업데이트
        likeCount.text(data.totalLikes);
        likerPhotosContainer.empty();
        if (data.recentLikerPhotos && data.recentLikerPhotos.length > 0) {
            const photosHtml = data.recentLikerPhotos.map(photo => `<img src="${photo}" class="liker-profile-img">`).join('');
            likerPhotosContainer.html(photosHtml);
        }
    }

    function escapeHTML(str) {
        const p = document.createElement('p');
        p.textContent = str;
        return p.innerHTML;
    }

    function calculateTimeAgo(dateString) {
        const now = new Date();
        const postDate = new Date(dateString);
        const seconds = Math.floor((now.getTime() - postDate.getTime()) / 1000);
        if (seconds < 0) {
            return "방금 전";
        }
        let interval = seconds / 31536000;
        if (interval > 1) {
            return Math.floor(interval) + "년 전";
        }
        interval = seconds / 2592000;
        if (interval > 1) {
            return Math.floor(interval) + "개월 전";
        }
        interval = seconds / 86400;
        if (interval > 1) {
            return Math.floor(interval) + "일 전";
        }
        interval = seconds / 3600;
        if (interval > 1) {
            return Math.floor(interval) + "시간 전";
        }
        interval = seconds / 60;
        if (interval > 1) {
            return Math.floor(interval) + "분 전";
        }
        return "방금 전";
    }
}