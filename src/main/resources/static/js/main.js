// src/main/resources/static/js/main.js

$(document).ready(function () {
    let finalAreaKey = null;

    // 쿼리 파라미터에서 key 추출
    const urlParams = new URLSearchParams(window.location.search);
    const areaKeyFromUrl = urlParams.get('areaKey');

    if (areaKeyFromUrl) {
        console.log('URL 파라미터에서 key 발견:', areaKeyFromUrl);
        finalAreaKey = areaKeyFromUrl;
    } else {
        // 쿼리 파라미터가 없으면 localStorage 값 참조
        const areaKeyFromStorage = localStorage.getItem('currentAreaKey');
        if (areaKeyFromStorage) {
            console.log('localStorage에서 key 발견:', areaKeyFromStorage);
            finalAreaKey = areaKeyFromStorage;
        }
    }

    const locationTextElement = $('#current-location-text');

    if (finalAreaKey) {
        // 상단바 위치 정보 표시
        // 주소지가 출력되야 해서 위치 전송 기능 개발되면 그에 맞게 변경되야 함!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        locationTextElement.text(finalAreaKey);

        // finalAreaKey를 사용해 바로 게시물을 로드
        loadPosts(finalAreaKey);
    } else {
        console.log("게시물을 불러올 지역 정보가 없습니다.");
        locationTextElement.text("지역 설정 중..");
    }

    // 포스트 컨테이너에 이벤트 위임 방식으로 댓글 관련 이벤트 바인딩
    $('#post-list-container').on('click', '.comment-trigger', async function (e) {
        e.preventDefault();
        const postCard = $(this).closest('.post-card');
        const postId = postCard.data('post-id');
        const commentSection = postCard.find('.comment-section');

        const isHidden = commentSection.is(':hidden');
        commentSection.toggle(200);

        if (isHidden && postCard.find('.comment-list').is(':empty')) {
            await loadComments(postId, postCard);
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
            console.error('Failed to post comment:', error);
            alert('댓글 작성에 실패했습니다.');
        } finally {
            submitButton.prop('disabled', false);
        }
    });

    // 좋아요 버튼 클릭 이벤트 리스너
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
                console.error('좋아요 토글 실패:', result.message);
                alert('좋아요 기능에 문제가 발생했습니다.');
            }
        } catch (error) {
            console.error('네트워크 또는 기타 오류:', error);
            alert('좋아요 요청 중 오류가 발생했습니다.');
        }
    });
});

// key를 이용해 게시물을 불러오는 함수
function loadPosts(key) {
    console.log(`'${key}' 지역의 게시물을 불러옵니다.`);

    $.ajax({
        url: `/forum/area/${key}`,
        type: 'GET',
        dataType: 'json',
        success: function (result) {
            if (result.status === '000' && result.data) {
                const posts = result.data;
                const container = $('#post-list-container');
                container.empty();

                if (posts.length === 0) {
                    container.html('<p>해당 지역에는 작성된 포럼이 없습니다.</p>');
                } else {
                    posts.forEach(post => {
                        const postHtml = createPostHtml(post);
                        container.append(postHtml);
                    });
                }
            } else {
                console.error('게시물 조회 실패:', result.message);
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error('네트워크 오류:', textStatus, errorThrown);
        }
    });
}

// 게시물 HTML 생성 함수
function createPostHtml(post) {

    // 여러 이미지 표시를 위한 HTML 생성
    const imagesHtml = post.imageUrls && post.imageUrls.length > 0 ?
        post.imageUrls.map(url => `<img alt="게시물 사진" class="post-image" src="${url}">`).join('')
        : '';

    //시간 표시 로직
    const timeAgoText = calculateTimeAgo(post.createdAt);

    // 좋아요 상태에 따라 하트 아이콘을 결정
    const likeIcon = post.isLiked ? '❤️' : '♡';

    // 최근 좋아요 누른 사람들의 프로필 사진 HTML 생성
    const recentLikerPhotosHtml = (post.recentLikerPhotos && post.recentLikerPhotos.length > 0) ?
        post.recentLikerPhotos.map(photo => `<img src="${photo}" class="liker-profile-img">`).join('')
        : '';

    return `
       <div class="post-card" data-post-id="${post.id}">
            <div class="post-author">
                <img alt="${post.writerNickname}" src="${post.writerProfilePhotoUrl}" class="profile-img">
                <div class="post-author-info">
                    <div class="name">${post.writerNickname}</div>
                    <div class="time">${timeAgoText}</div>
                </div>
            </div>
            <div class="post-content">
                <p>${escapeHTML(post.contentsText)}</p>
                ${imagesHtml}
            </div>
            <div class="post-actions">
                <div>
                    <span class="like-button" data-forum-id="${post.id}">
                        <span class="like-icon">${likeIcon}</span>
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

// 좋아요 상태 업데이트 함수
function updateLikeStatus(likeButton, data) {
    const likeIcon = likeButton.find('.like-icon');
    const likeCount = likeButton.find('.like-count');
    const postCard = likeButton.closest('.post-card');
    const likerPhotosContainer = postCard.find('.liker-photos');

    // 하트 아이콘 변경
    likeIcon.text(data.isLiked ? '❤️' : '♡');

    // 좋아요 개수 변경
    likeCount.text(data.totalLikes);

    // 최근 좋아요 누른 사람들의 프로필 사진 업데이트
    likerPhotosContainer.empty();
    if (data.recentLikerPhotos && data.recentLikerPhotos.length > 0) {
        const photosHtml = data.recentLikerPhotos.map(photo => `<img src="${photo}" class="liker-profile-img">`).join('');
        likerPhotosContainer.html(photosHtml);
    }
}

// 댓글 불러오기 함수
async function loadComments(postId, postCard) {
    try {
        const response = await fetch(`/api/forum/${postId}/comments`);
        if (!response.ok) throw new Error('Failed to load comments');

        const comments = await response.json();
        const commentList = postCard.find('.comment-list');
        commentList.empty(); // 기존 내용 초기화
        comments.forEach(comment => addCommentToDOM(comment, postCard, false));
        updateCommentCount(postCard);
    } catch (error) {
        console.error('Error loading comments:', error);
    }
}

// DOM에 댓글 추가 함수
function addCommentToDOM(comment, postCard, prepend = true) {
    const commentList = postCard.find('.comment-list');
    const item = $('<li>').addClass('comment-item');
    item.html(`
        <img src="${comment.authorProfileImageUrl}" alt="${comment.authorNickname}">
        <div class="comment-bubble">
            <div class="author">${comment.authorNickname}</div>
            <div class="content">${escapeHTML(comment.content)}</div>
        </div>
    `);
    if (prepend) {
        commentList.prepend(item);
    } else {
        commentList.append(item);
    }
}

// 댓글 카운트 업데이트 함수
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

// HTML 태그 이스케이프 함수 (XSS 방지)
function escapeHTML(str) {
    const p = document.createElement('p');
    p.textContent = str;
    return p.innerHTML;
}

// 시간 변환 유틸리티 함수
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