import {stopBackgroundTracking} from "../modules/location-tracker";

export function initProfile() {

    // --- 1. UI 요소 및 상태 변수 정의 ---
    const openEditModalBtn = document.getElementById('open-edit-modal-btn');
    const editProfileModal = document.getElementById('edit-profile-modal');
    const cancelEditBtn = document.getElementById('cancel-edit-btn');
    const openPasswordModalBtn = document.getElementById('open-password-modal-btn');
    const nicknameInput = document.getElementById('nickname');
    const checkNicknameBtn = document.getElementById('check-nickname-btn');
    const nicknameFeedback = document.getElementById('nickname-feedback');
    const changePasswordModal = document.getElementById('change-password-modal');
    const newPasswordInput = document.getElementById('new-password');
    const confirmPasswordInput = document.getElementById('confirm-password');
    const passwordFeedback = document.getElementById('password-feedback');
    const updatePasswordBtn = document.getElementById('update-password-btn');
    const cancelPasswordBtn = document.getElementById('cancel-password-btn');
    const changePasswordForm = document.getElementById('change-password-form');
    const editProfileForm = document.getElementById('edit-profile-form');
    const tabFind = document.getElementById('tab-find');
    const tabForum = document.getElementById('tab-forum');
    const carousel = document.getElementById('carousel');
    const carouselWrapper = document.getElementById('carousel-wrapper');
    const profileBody = document.getElementById('page-profile');
    const profileNickname = profileBody.dataset.profileNickname;

    // [변경사항] 팔로우 버튼이 헤더 우측으로 이동하며, DOM 내 여러 위치/조건에 존재할 수 있어
    // querySelectorAll로 모두 바인딩하도록 수정했습니다.
    const followBtns = document.querySelectorAll('.follow-btn'); // <-- 변경 포인트
    // 기존: const followBtn = document.querySelector('.follow-btn');

    const profileImageInput = document.getElementById('profile-image-upload');
    const profileImage = document.querySelector('.profile-info__pic');
    const isMyProfile = (profileBody.dataset.isMyProfile === 'true'); // 없으면 false
    const logoutBtn = document.getElementById('logout-btn');

    console.log("Is this my profile?", isMyProfile)
    // 상태 관리 변수
    const initialTab = tabFind ? 'find' : 'forum';
    let currentTab = initialTab;
    let currentPageIndex = 0;
    const postsPerPage = 2;
    let isNicknameAvailable = false;
    let newPassword = null;
    let touchStartX = 0;

    const state = {
        find: {content: [], page: 0, totalPages: 1, isLoading: false},
        forum: {content: [], page: 0, totalPages: 1, isLoading: false}
    };

    // ===== 유틸 =====
    const esc = (s) => String(s ?? '').replace(/[&<>"']/g, m => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[m]));
    const hasImg = (u) => !!u && String(u).trim() !== '' && String(u).trim().toLowerCase() !== 'null';
    const snippet = (raw, max = 120) => {
        const text = String(raw ?? '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
        return text.length <= max ? text : text.slice(0, max) + '…';
    };
    const SNIPPET_STYLE = 'padding:8px 10px;font-size:14px;line-height:1.45;color:#111;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden;white-space:normal;';

    // 새로운 유틸리티 함수 시작
    function formatTimeAgo(isoString) {
        if (!isoString) return '';
        const now = new Date();
        const past = new Date(isoString);
        const diffInSeconds = Math.floor((now - past) / 1000);

        if (diffInSeconds < 60) return `${diffInSeconds}초 전`;
        if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}분 전`;
        if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}시간 전`;

        // 날짜만 표시 (예: 10/01)
        const year = past.getFullYear();
        const month = String(past.getMonth() + 1).padStart(2, '0');
        const day = String(past.getDate()).padStart(2, '0');

        return `${year}년 ${month}월 ${day}일`;
    }

    function getRemainingTime(expiresAt) {
        if (!expiresAt) return null;
        const now = new Date().getTime();
        const expiry = new Date(expiresAt).getTime();
        const diffInSeconds = Math.floor((expiry - now) / 1000);

        if (diffInSeconds <= 0) return {expired: true, text: '만료됨'};

        const hours = Math.floor(diffInSeconds / 3600);
        const minutes = Math.floor((diffInSeconds % 3600) / 60);

        if (hours > 0) return {expired: false, text: `${hours}시간 남음`};
        if (minutes > 0) return {expired: false, text: `${minutes}분 남음`};

        // 1분 미만 남았을 경우
        return {expired: false, text: `<1분 남음`};
    }

    // --- 2. 데이터 로딩 (API 호출) ---
    async function loadPosts(tab, page) {
        const tabState = state[tab];
        if (tabState.isLoading || page >= tabState.totalPages) return;

        tabState.isLoading = true;
        try {
            const endpoint = tab === 'find'
                ? `/profile/findlist/${profileNickname}?page=${page}&size=4`
                : `/profile/forumlist/${profileNickname}?page=${page}&size=4`;


            const response = await fetch(endpoint);
            if (!response.ok) throw new Error('Failed to fetch posts');

            const data = await response.json();
            tabState.content.push(...data.content);
            tabState.totalPages = data.totalPages;
            tabState.page = page; // 현재 로드된 백엔드 페이지 번호 저장

            renderCarousel();
        } catch (error) {
            console.error(`Error loading ${tab} posts:`, error);
        } finally {
            tabState.isLoading = false;
        }
    }

    async function loadMyPark() {
        const guestbookWrapper = document.querySelector('.guestbook-wrapper');
        if (!guestbookWrapper) return;
        const guestbookContent = guestbookWrapper.querySelector('.guestbook__content');

        try {
            const response = await fetch(`/profile/park/${profileNickname}`);
            if (response.ok) {
                const parkData = await response.json();
                const guestbookSection = guestbookWrapper.querySelector('.guestbook');
                guestbookSection.innerHTML = '';

                const img = document.createElement('img');
                img.src = parkData.contentCaptureUrl;
                img.alt = 'My Park Guestbook Image';
                img.style.width = '100%';
                img.style.borderRadius = '12px';
                img.style.display = 'block';
                guestbookSection.appendChild(img);
            } else {
                if (guestbookContent) guestbookContent.textContent = '작성된 Park 방명록이 없습니다.';
            }
        } catch (error) {
            console.error('Error loading Park data:', error);
            if (guestbookContent) guestbookContent.textContent = 'Park를 불러오는 중 오류가 발생했습니다.';
        }
    }

    // --- 3. 렌더링 (화면 그리기) ---
    function renderCarousel() {
        if (!carousel) return;

        const tabState = state[currentTab];
        const data = tabState.content;
        const totalPosts = data.length;
        const totalCarouselPages = Math.ceil(totalPosts / postsPerPage) || 1;

        carousel.innerHTML = '';
        if (totalPosts === 0 && !tabState.isLoading) {
            carousel.innerHTML = `<div class="empty-posts-message">게시물이 없습니다.</div>`;
            return;
        }

        for (let i = 0; i < totalCarouselPages; i++) {
            const pageElement = document.createElement('div');
            pageElement.className = 'content-page';
            let pageHTML = '';

            const pageData = data.slice(i * postsPerPage, (i + 1) * postsPerPage);
            pageData.forEach(post => {
                let link = '';

                if (currentTab === 'forum') {
                    // ✅ 컨트롤러가 /forum/feed 만 있으니, 피드로 보내고 앵커로 해당 글 위치
                    link = `/forum/feed#post-${post.id}`;
                } else {
                    // ✅ 네가 새로 추가한 매핑에 맞춤: /find/original/{id}
                    link = `/find/feed/${post.id}`;
                }

                console.log("Post ID:", post.id, "CreatedAt:", post.createdAt, "ExpiresAt:", post.expiresAt);

                // 나머지 렌더링 로직은 그대로
                const statusIcon = (currentTab === 'find' && post.isExpiring)
                    ? '<div class="post-item__status-icon">⏰</div>' : '';

                // --- Forum 전용: 이미지가 없으면 텍스트로 렌더, 이미지 깨지면 onerror로 텍스트 대체 ---
                if (currentTab === 'forum') {
                    const imgOk = hasImg(post.imageUrl);
                    const snip = snippet(post.contentsText, 120);

                    const imgHtml = imgOk
                        ? `<img class="post-item__image"
                                src="${esc(post.imageUrl)}"
                                alt="Post image"
                                loading="lazy"
                                data-snippet="${esc(snip)}"
                                onerror="
                                  this.style.display='none';
                                  var p=document.createElement('div');
                                  p.className='post-item__text';
                                  p.setAttribute('style','${SNIPPET_STYLE}');
                                  p.textContent=this.getAttribute('data-snippet')||'';
                                  this.parentElement.appendChild(p);
                                ">`
                        : `<div class="post-item__text" style="${SNIPPET_STYLE}">${esc(snip)}</div>`;

                    pageHTML += `
                        <div class="post-item-container"> 
                            <div class="post-item-block"> 
                                <a href="${link}" class="post-item">
                                    ${imgHtml}
                                    <p class="post-item__location">📍 ${esc(post.location || '위치 정보 없음')}</p>
                                </a>
                            </div>
                        </div>`;
                } else {
                    // 기존 Fin'd 렌더 (이미지 전제)

                    // Fin'd 탭: 작성 시간 및 유효 기간 표시 로직
                    let timeInfoHTML = '';
                    if (post.createdAt) {
                        const postedTime = formatTimeAgo(post.createdAt);
                        const remaining = getRemainingTime(post.expiresAt);

                        let expiryDisplay = '';
                        // 만료되지 않았을 때만 빨간색으로 남은 시간 표시
                        if (remaining && !remaining.expired) {
                            expiryDisplay = `<span class="expiry-time-remaining">${remaining.text}</span>`;
                        }

                        // post-item__time-info는 이미지 바깥에 위치해야 하므로,
                        // post-item__location이 이미지 안에 위치하도록 HTML 구조를 다시 잡습니다.
                        timeInfoHTML = `
                            <div class="post-item-time-info-wrapper"> 
                                <div class="post-item__time-info">
                                    <span class="post-item__posted-time">${postedTime}</span>
                                    ${expiryDisplay}
                                </div>
                            </div>
                        `;
                    }

                    pageHTML += `
                        <div class="post-item-container"> <div class="post-item-block"> 
                                <a href="${link}" class="post-item">
                                    <img class="post-item__image" src="${esc(post.imageUrl)}" alt="Post image" loading="lazy">
                                    ${statusIcon}
                                    <p class="post-item__location">📍 ${esc(post.location || '')}</p> 
                                </a>
                            </div>
                            ${timeInfoHTML} 
                        </div>`;
                }
            });

            pageElement.innerHTML = pageHTML;
            carousel.appendChild(pageElement);
        }
        // 캐러셀 페이지 수에 맞게 transform 재조정
        goToPage(currentPageIndex, true);
    }

    // --- 4. UI 로직 (모달, 탭, 캐러셀) ---
    function openModal(modal) {
        if (modal) modal.style.display = 'flex';
    }

    function closeModal(modal) {
        if (modal) modal.style.display = 'none';
    }

    function goToPage(pageIndex, force = false) {
        if (!carousel) return;

        const tabState = state[currentTab];
        const totalCarouselPages = Math.ceil(tabState.content.length / postsPerPage) || 1;

        if (!force) {
            if (pageIndex < 0) pageIndex = 0;
            if (pageIndex >= totalCarouselPages) {
                if (tabState.page < tabState.totalPages - 1) {
                    loadPosts(currentTab, tabState.page + 1);
                }
                pageIndex = totalCarouselPages - 1;
            }
        }

        currentPageIndex = pageIndex;
        const offset = -currentPageIndex * 100;
        carousel.style.transform = `translateX(${offset}%)`;
    }

    function switchTab(tab) {
        if (currentTab === tab) return;
        currentTab = tab;

        if (tabFind) tabFind.classList.toggle('active', tab === 'find');
        if (tabForum) tabForum.classList.toggle('active', tab === 'forum');

        currentPageIndex = 0;

        // 🌟🌟🌟 수정: 탭 전환 시 캐러셀 내용을 즉시 비웁니다. 🌟🌟🌟
        if (carousel) carousel.innerHTML = '';

        if (state[tab].content.length === 0) {
            loadPosts(tab, 0);
        } else {
            renderCarousel();
        }
    }

    function validatePassword() {
        if (newPasswordInput.value && newPasswordInput.value === confirmPasswordInput.value) {
            passwordFeedback.textContent = '비밀번호가 일치합니다.';
            passwordFeedback.style.color = 'green';
            updatePasswordBtn.disabled = false;
        } else {
            passwordFeedback.textContent = '비밀번호가 일치하지 않습니다.';
            passwordFeedback.style.color = 'red';
            updatePasswordBtn.disabled = true;
        }
    }

    // --- 5. 이벤트 리스너 연결 ---
    if (openEditModalBtn) openEditModalBtn.addEventListener('click', () => openModal(editProfileModal));
    if (cancelEditBtn) cancelEditBtn.addEventListener('click', () => closeModal(editProfileModal));
    if (openPasswordModalBtn) openPasswordModalBtn.addEventListener('click', (e) => {
        e.preventDefault();
        closeModal(editProfileModal);
        openModal(changePasswordModal);
    });
    if (cancelPasswordBtn) cancelPasswordBtn.addEventListener('click', () => {
        closeModal(changePasswordModal);
        openModal(editProfileModal);
    });

    if (checkNicknameBtn) checkNicknameBtn.addEventListener('click', async () => {
        const nickname = nicknameInput.value;
        if (!nickname) {
            nicknameFeedback.textContent = '닉네임을 입력해주세요.';
            nicknameFeedback.style.color = 'red';
            isNicknameAvailable = false;
            return;
        }

        try {
            // 실제 서버 API를 호출하여 닉네임 중복 여부를 확인합니다.
            const response = await fetch(`/check-nickname?nickname=${encodeURIComponent(nickname)}`);
            if (!response.ok) throw new Error('Server error');
            const data = await response.json();

            if (data.available) {
                nicknameFeedback.textContent = '사용 가능한 닉네임입니다.';
                nicknameFeedback.style.color = 'green';
                isNicknameAvailable = true;
            } else {
                nicknameFeedback.textContent = '이미 사용 중인 닉네임입니다.';
                nicknameFeedback.style.color = 'red';
                isNicknameAvailable = false;
            }
        } catch (error) {
            nicknameFeedback.textContent = '오류가 발생했습니다.';
            nicknameFeedback.style.color = 'red';
            isNicknameAvailable = false;
            console.error("Nickname check failed:", error);
        }

        setTimeout(() => {
            if (nickname.toLowerCase() === 'admin') {
                nicknameFeedback.textContent = '닉네임 변경 불가!';
                nicknameFeedback.style.color = 'red';
                isNicknameAvailable = false;
            } else {
                nicknameFeedback.textContent = '닉네임 변경 가능!';
                nicknameFeedback.style.color = 'green';
                isNicknameAvailable = true;
            }
        }, 500);
    });

    if (newPasswordInput) newPasswordInput.addEventListener('input', validatePassword);
    if (confirmPasswordInput) confirmPasswordInput.addEventListener('input', validatePassword);

    // (버그 수정) submit 리스너 중첩 제거
    if (changePasswordForm) changePasswordForm.addEventListener('submit', (e) => {
        e.preventDefault();
        newPassword = newPasswordInput.value;
        alert('비밀번호가 임시 저장되었습니다. Update 버튼을 눌러 최종 적용하세요.');
        closeModal(changePasswordModal);
        openModal(editProfileModal);
    });

    if (editProfileForm) editProfileForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        let updateMessage = '프로필 업데이트:';
        if (isNicknameAvailable && nicknameInput.value) {
            // CSRF 토큰 헤더 준비
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            const headers = {'Content-Type': 'application/json'};
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            try {
                const response = await fetch('/profile/edit/nickname', {
                    method: 'POST',
                    headers: headers,
                    body: JSON.stringify({nickname: nicknameInput.value})
                });

                if (response.ok) {
                    alert('닉네임이 성공적으로 변경되었습니다. 페이지를 새로고침합니다.');
                    location.reload(); // 성공 시 페이지를 새로고침하여 변경사항 반영
                } else {
                    const errorData = await response.json();
                    alert('닉네임 변경에 실패했습니다: ' + (errorData.message || '서버 오류'));
                }
            } catch (error) {
                console.error('Error updating nickname:', error);
                alert('닉네임 변경 중 오류가 발생했습니다.');
            }
        } else {
            alert('닉네임 중복 확인을 먼저 완료해주세요.');
        }
    });

    if (tabFind) tabFind.addEventListener('click', () => switchTab('find'));
    if (tabForum) tabForum.addEventListener('click', () => switchTab('forum'));

    if (carouselWrapper) {
        carouselWrapper.addEventListener('touchstart', (e) => {
            touchStartX = e.touches[0].clientX;
        }, {passive: true});
        carouselWrapper.addEventListener('touchend', (e) => {
            const touchEndX = e.changedTouches[0].clientX;
            const swipeDistance = touchEndX - touchStartX;
            if (swipeDistance < -50) goToPage(currentPageIndex + 1);
            else if (swipeDistance > 50) goToPage(currentPageIndex - 1);
        }, {passive: true});
    }

    let isMouseDown = false;
    if (carouselWrapper) {
        const endDrag = (e) => {
            if (!isMouseDown) return;
            isMouseDown = false;
            carouselWrapper.classList.remove('dragging');
            carousel.style.transition = 'transform 0.3s ease-in-out';

            const touchEndX = e.clientX;
            const swipeDistance = touchEndX - touchStartX;

            if (swipeDistance < -50) {
                goToPage(currentPageIndex + 1);
            } else if (swipeDistance > 50) {
                goToPage(currentPageIndex - 1);
            } else {
                goToPage(currentPageIndex, true);
            }
        };

        carouselWrapper.addEventListener('mousedown', (e) => {
            isMouseDown = true;
            touchStartX = e.clientX;
            carouselWrapper.classList.add('dragging');
            e.preventDefault();
        });

        carouselWrapper.addEventListener('mousemove', (e) => {
            if (!isMouseDown) return;
            const currentX = e.clientX;
            const distance = currentX - touchStartX;

            const baseOffset = -currentPageIndex * 100;
            carousel.style.transition = 'none';
            carousel.style.transform = `translateX(calc(${baseOffset}% + ${distance}px))`;
        });

        carouselWrapper.addEventListener('mouseup', endDrag);
        carouselWrapper.addEventListener('mouseleave', endDrag);
    }

    // [변경사항] 여러 팔로우 버튼을 모두 바인딩합니다. (우측 액션 영역 등 위치가 변해도 동작)
    if (followBtns && followBtns.length) {
        followBtns.forEach((btn) => {
            btn.addEventListener('click', async (event) => {
                const button = event.currentTarget;
                const userId = button.dataset.userid;
                const isFollowing = button.classList.contains('unfollow');

                const url = isFollowing ? '/friend/unfollowing' : '/friend/addfollowing';
                const method = isFollowing ? 'DELETE' : 'POST';

                const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
                const headers = {'Content-Type': 'application/json'};
                if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

                try {
                    const response = await fetch(url, {
                        method: method,
                        headers: headers,
                        body: JSON.stringify({userId: userId})
                    });

                    if (response.ok) {
                        if (isFollowing) {
                            button.classList.replace('unfollow', 'follow');
                            button.textContent = 'Follow';
                        } else {
                            button.classList.replace('follow', 'unfollow');
                            button.textContent = 'Following';
                        }
                    } else {
                        alert('요청 처리 중 오류가 발생했습니다.');
                    }
                } catch (error) {
                    console.error('Follow error:', error);
                }
            });
        });
    }

    if (isMyProfile && profileImageInput) {
        console.log("Attaching image upload event listener.");
        profileImageInput.addEventListener('change', async (event) => {
            const file = event.target.files[0];
            if (!file) return;

            const formData = new FormData();
            formData.append('profileImage', file);

            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            const headers = {};
            if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

            try {
                const response = await fetch('/profile/image', {
                    method: 'POST',
                    headers: headers, // FormData 전송 시 Content-Type은 브라우저가 자동으로 설정되므로 넣지 않습니다.
                    body: formData
                });

                if (response.ok) {
                    const result = await response.json();
                    profileImage.src = result.imageUrl + '?t=' + new Date().getTime();
                    alert('프로필 이미지가 성공적으로 변경되었습니다.');
                } else {
                    const errorResult = await response.json();
                    alert('이미지 변경에 실패했습니다: ' + (errorResult.message || '서버 오류'));
                }
            } catch (error) {
                console.error('Error uploading profile image:', error);
                alert('이미지 업로드 중 오류가 발생했습니다.');
            }
        });
    }

    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            try {
                const response = await fetch('/logout', {method: 'POST'});
                if (response.ok && response.redirected) {
                    window.location.href = '/login';
                } else if (response.ok) {
                    // 일부 설정에서는 redirected가 false일 수 있음
                    window.location.href = '/login';
                }
                await stopBackgroundTracking();
            } catch (error) {
                console.error('Error during logout:', error);
            }
        });
    }

    // --- 6. 초기 실행 ---
    loadMyPark();
    loadPosts(initialTab, 0);
}
