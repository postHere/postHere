// src/main/resources/static/forumJs/main.js

$(document).ready(function () {
    let finalAreaKey = null;

    // 쿼리 파라미터 추출
    const urlParams = new URLSearchParams(window.location.search);
    const areaKeyFromUrl = urlParams.get('areaKey');

    if (areaKeyFromUrl) {
        console.log('URL 파라미터에서 key 발견:', areaKeyFromUrl);
        finalAreaKey = areaKeyFromUrl;
        // 로컬스토리지에 저장된 값이 없거나 다르다면 업데이트
        if (localStorage.getItem('currentAreaKey') !== areaKeyFromUrl) {
            localStorage.setItem('currentAreaKey', areaKeyFromUrl);
        }
    } else {
        // 쿼리 파라미터가 없으면 localStorage 값을 참조
        const areaKeyFromStorage = localStorage.getItem('currentAreaKey');
        if (areaKeyFromStorage) {
            console.log('localStorage에서 key 발견:', areaKeyFromStorage);
            finalAreaKey = areaKeyFromStorage;
        }
    }

    if (finalAreaKey) {
        loadPosts(finalAreaKey);
    } else {
        console.log("게시물을 불러올 지역 정보가 없습니다. 기본 지역을 설정하세요.");
    }
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
                container.empty(); // 기존 게시물 목록을 비웁니다.

                if (posts.length === 0) {
                    // 게시물이 없을 경우의 UI
                    container.html('<p>해당 지역에는 작성된 포럼이 없습니다.</p>');
                } else {
                    // 게시물 목록을 순회하며 HTML을 생성
                    posts.forEach(post => {
                        const postHtml = `
                    <div class="post-container">
                        <div class="post-user-info">
                            <img src="/path/to/profile-img.jpg" alt="프로필" class="profile-img">
                            <div>
                                <span class="user-name">${post.writerNickname}</span>
                                <div class="post-meta">
                                    <span class="post-time">방금 전</span> •
                                    <span class="post-location">${post.location}</span>
                                </div>
                            </div>
                        </div>
                        <div class="post-body">
                            <p class="post-text">${post.contentsText}</p>
                            ${post.imageUrls && post.imageUrls.length > 0 ?
                            `<img src="${post.imageUrls[0]}" alt="게시물 사진" class="post-image">` : ''}
                        </div>
                        <div class="post-actions">
                            <span>❤️ 21 likes</span>
                            <span>💬 4 comments</span>
                        </div>
                    </div>
                `;
                        container.append(postHtml);
                    });
                }
            } else {
                console.error('게시물 조회 실패:', result.message);
                // 오류 UI 로직
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error('네트워크 오류:', textStatus, errorThrown);
        }
    });
}