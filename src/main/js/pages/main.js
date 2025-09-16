export function initMain() {
    let finalAreaKey = null;
    const urlParams = new URLSearchParams(window.location.search);
    const areaKeyFromUrl = urlParams.get('areaKey');

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

    $('#post-list-container').on('click', '.comment-trigger', async function (e) {
        e.preventDefault();
        const postCard = $(this).closest('.post-card');
        const commentSection = postCard.find('.comment-section');
        commentSection.toggle(200);
    });

    $('#post-list-container').on('click', '.comment-delete-button', async function () {
        const button = $(this);
        const commentId = button.data('comment-id');
        const postCard = button.closest('.post-card');
        const postId = postCard.data('post-id');
        if (confirm("댓글을 삭제하시겠습니까?")) {
            try {
                const response = await fetch(`/api/forum/${postId}/comments/${commentId}`, {
                    method: 'DELETE'
                });
                if (response.status === 204) {
                    button.closest('.comment-item').remove();
                    updateCommentCount(postCard);
                    alert('댓글이 삭제되었습니다.');
                } else if (response.status === 401) {
                    alert('로그인이 필요합니다.');
                    window.location.href = '/login';
                } else if (response.status === 403) {
                    alert('댓글 삭제 권한이 없습니다.');
                } else {
                    throw new Error('댓글 삭제에 실패했습니다.');
                }
            } catch (error) {
                alert('삭제 중 오류가 발생했습니다.');
            }
        }
    });

    $('#post-list-container').on('submit', '.comment-form', async function (e) {
        e.preventDefault();
        const form = $(this);
        const postCard = form.closest('.post-card');
        const postId = postCard.data('post-id');
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
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const newComment = await response.json();
            addCommentToDOM(newComment, postCard);
            input.val('');
            updateCommentCount(postCard);
        } catch (error) {
            alert('댓글 작성에 실패했습니다.');
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
                    alert('로그인이 필요합니다.');
                    window.location.href = '/login';
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const result = await response.json();
            if (result.status === '000') {
                updateLikeStatus(likeButton, result.data);
            } else {
                alert('좋아요 기능에 문제가 발생했습니다.');
            }
        } catch (error) {
            alert('좋아요 요청 중 오류가 발생했습니다.');
        }
    });

    // 변경: 모달창을 '...' 버튼 바로 아래에 생성하는 로직으로 변경
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
        modal.on('click', '.delete-button', function () {
            modal.remove();
            $('#confirm-delete-modal').data('current-post-id', postId).show();
        });
        modal.on('click', '.edit-button', function () {
            modal.remove();
            window.location.href = `/forum/${postId}/edit`;
        });
    });

    $('#confirm-delete-yes').on('click', async function () {
        const postId = $('#confirm-delete-modal').data('current-post-id');
        try {
            const response = await fetch(`/forum/${postId}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                throw new Error('게시글 삭제에 실패했습니다.');
            }
            alert('게시글이 성공적으로 삭제되었습니다.');
            $(`.post-card[data-post-id="${postId}"]`).remove();
            $('#confirm-delete-modal').hide();
        } catch (error) {
            alert('삭제 중 오류가 발생했습니다.');
        }
    });

    $('.modal .close-button, #confirm-delete-no').on('click', function () {
        $(this).closest('.modal').hide();
    });

    function createEmptyPostHtml() {
        const imagePath = '../images/map-icon.png';
        return `
            <div class="empty-forum-container">
                <img src="${imagePath}" alt="Map icon" class="empty-icon">
                <p class="empty-text">해당 지역에는 작성된 Forum이 없어요.</p>
                <p class="empty-subtext">다른 이야기를 만나고 싶다면,<br>발걸음을 옮기거나 상단 아이콘을 눌러 함께해요.</p>
            </div>
        `;
    }

    function loadPosts(key) {
        $.ajax({
            url: `/forum/area/${key}`,
            type: 'GET',
            dataType: 'json',
            success: function (result) {
                const container = $('#post-list-container');
                container.empty();
                if (result.status === '000' && result.data && result.data.length > 0) {
                    result.data.forEach(post => {
                        const postHtml = createPostHtml(post);
                        container.append(postHtml);
                        const newPostCard = container.find(`.post-card[data-post-id="${post.id}"]`);
                        initCarousel(newPostCard);

                        // 게시글 로드 후 댓글도 함께 불러옴
                        loadComments(post.id, newPostCard);
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

    // 댓글 목록을 불러와서 표시하는 함수
    async function loadComments(postId, postCard) {
        try {
            const response = await fetch(`/api/forum/${postId}/comments`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            const comments = await response.json();
            const commentList = postCard.find('.comment-list');
            commentList.empty(); // 기존 댓글 비우기

            // 댓글 데이터를 순회하며 DOM에 추가
            if (comments && comments.length > 0) {
                comments.forEach(comment => addCommentToDOM(comment, postCard));
            }
        } catch (error) {
            console.error('Failed to load comments:', error);
            alert('댓글을 불러오는 데 실패했습니다.');
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

    // 변경: 하트 아이콘을 유니코드 문자로 직접 사용하도록 수정
    function createPostHtml(post) {
        const imagesHtml = post.imageUrls && post.imageUrls.length > 0 ? `
            <div class="post-image-carousel">
                <div class="carousel-images-container">
                    ${post.imageUrls.map(url => `<img alt="게시물 사진" class="post-image" src="${url}">`).join('')}
                </div>
                <div class="carousel-indicators">
                    ${post.imageUrls.map((_, index) => `<span class="indicator ${index === 0 ? 'active' : ''}"></span>`).join('')}
                </div>
                ${post.imageUrls.length > 1 ? `<button class="carousel-prev-btn">&lt;</button>
                <button class="carousel-next-btn">&gt;</button>` : ''}
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
        const likeButtonClass = post.liked ? 'like-button liked' : 'like-button';
        const heartIcon = post.liked ? '♥' : '♡'; // 변경: 유니코드 하트 문자 사용
        return `
           <div class="post-card" data-post-id="${post.id}">
                <div class="post-header">
                    <div class="post-author">
                        <img alt="${post.writerNickname}" src="${post.writerProfilePhotoUrl}" class="profile-img">
                        <div class="post-author-info">
                            <div class="name">${post.writerNickname}</div>
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
                <div class="comment-section" style="display:none;">
                    <ul class="comment-list"></ul>
                    <form class="comment-form">
                        <input class="comment-input" placeholder="댓글을 입력하세요..." required type="text">
                        <button class="comment-submit" type="submit">게시</button>
                    </form>
                </div>
            </div>
        `;
    }

    // 변경: 하트 색상을 유니코드로 변경하도록 수정
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

    function addCommentToDOM(comment, postCard, prepend = true) {
        const commentList = postCard.find('.comment-list');

        // comment 객체의 'author' 필드를 사용하여 삭제 버튼 표시 여부 결정
        const deleteButtonHtml = comment.author ?
            `<button class="comment-delete-button" data-comment-id="${comment.id}">X</button>` : '';

        const itemHtml = `
            <img src="${comment.authorProfileImageUrl}" alt="${comment.authorNickname}" class="profile-img">
            <div class="comment-content-wrapper">
                <div class="comment-main">
                    <div class="comment-bubble">
                        <span class="author">${comment.authorNickname}</span>
                        <span class="content">${escapeHTML(comment.content)}</span>
                    </div>
                    ${deleteButtonHtml}
                </div>
            </div>
        `;

        const item = $('<li>').addClass('comment-item').html(itemHtml);
        if (prepend) {
            commentList.prepend(item);
        } else {
            commentList.append(item);
        }
    }

    function updateCommentCount(postCard) {
        const commentList = postCard.find('.comment-list');
        const count = commentList.children().length;
        const trigger = postCard.find('.comment-trigger');
        const commentCountSpan = postCard.find('.comment-count');
        if (count === 0) {
            trigger.text('댓글쓰기');
            trigger.addClass('no-comments');
        } else {
            trigger.removeClass('no-comments');
            commentCountSpan.text(`${count}`);
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