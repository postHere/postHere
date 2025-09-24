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
    const followBtn = document.querySelector('.follow-btn');
    const profileImageInput = document.getElementById('profile-image-upload');
    const profileImage = document.querySelector('.profile-info__pic');
    const isMyProfile = profileBody.dataset.isMyProfile === 'true';

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

    // --- 2. 데이터 로딩 (API 호출) ---
    async function loadPosts(tab, page) {
        const tabState = state[tab];
        if (tabState.isLoading || page >= tabState.totalPages) return;

        tabState.isLoading = true;
        try {
            const endpoint = tab === 'find'
                ? `/api/v1/users/${profileNickname}/finds?page=${page}&size=4`
                : `/api/v1/users/${profileNickname}/forums?page=${page}&size=4`;


            const response = await fetch(endpoint);
            if (!response.ok) throw new Error('Failed to fetch posts');

            const data = await response.json();
            tabState.content.push(...data.content);
            tabState.totalPages = data.totalPages;
            tabState.page = page; // 현재 로드된 백엔드 페이지 번호 저장


        } catch (error) {
            console.error(`Error loading ${tab} posts:`, error);
        } finally {
            tabState.isLoading = false;
            renderCarousel();
        }
    }

    async function loadMyPark() {
        const guestbookWrapper = document.querySelector('.guestbook-wrapper');
        if (!guestbookWrapper) return;
        const guestbookContent = guestbookWrapper.querySelector('.guestbook__content');

        try {
            const response = await fetch(`/api/v1/users/${profileNickname}/park`);
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
            carousel.innerHTML = `<div style="text-align:center;width:100%;color:grey;">게시물이 없습니다.</div>`;
            return;
        }

        //carousel.style.width = `${totalCarouselPages * 100}%`;

        for (let i = 0; i < totalCarouselPages; i++) {
            const pageElement = document.createElement('div');
            pageElement.className = 'content-page';
            let pageHTML = '';

            const pageData = data.slice(i * postsPerPage, (i + 1) * postsPerPage);
            pageData.forEach(post => {
                const link = currentTab === 'find' ? `/find-detail/${post.id}` : `/forum-detail/${post.id}`;
                const statusIcon = post.isExpiring ? '<div class="post-item__status-icon">⏰</div>' : '';

                pageHTML += `
                    <a href="${link}" class="post-item">
                        <img class="post-item__image" src="${post.imageUrl}" alt="Post image">
                        ${statusIcon}
                        <p class="post-item__location">📍 ${post.location}</p>
                    </a>`;
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

        if (!force) { // 일반 스와이프/클릭 시
            if (pageIndex < 0) pageIndex = 0;
            if (pageIndex >= totalCarouselPages) {
                // 마지막 페이지에 도달했고, 더 불러올 데이터가 있다면 다음 페이지 로드
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

        if (carousel) {
            carousel.innerHTML = `<div style="text-align:center;width:100%;color:grey;">로딩 중...</div>`;
        }

        if (tabFind) {
            tabFind.classList.toggle('active', tab === 'find');
        }
        tabForum.classList.toggle('active', tab === 'forum');

        currentPageIndex = 0; // 탭 전환 시 첫 페이지로

        const tabState = state[tab];
        
        // 이미 비어있는 탭이라고 확인된 경우(totalPages가 0),
        // 불필요한 API 호출 없이 즉시 '게시물이 없습니다'를 렌더링합니다.
        if (tabState.totalPages === 0) {
            renderCarousel();
            return;
        }

        if (tabState.content.length === 0) {
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

    if (checkNicknameBtn) checkNicknameBtn.addEventListener('click', () => {
        const nickname = nicknameInput.value;
        if (!nickname) {
            nicknameFeedback.textContent = '닉네임을 입력해주세요.';
            nicknameFeedback.style.color = 'red';
            isNicknameAvailable = false;
            return;
        }
        // This is a simulation of an API call
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

    if (changePasswordForm) changePasswordForm.addEventListener('submit', (e) => {
        if (changePasswordForm) changePasswordForm.addEventListener('submit', (e) => {
            e.preventDefault();
            newPassword = newPasswordInput.value;
            alert('비밀번호가 임시 저장되었습니다. Update 버튼을 눌러 최종 적용하세요.');
            closeModal(changePasswordModal);
            openModal(editProfileModal);
        });
    });

    if (editProfileForm) editProfileForm.addEventListener('submit', (e) => {
        e.preventDefault();
        let updateMessage = '프로필 업데이트:';
        if (isNicknameAvailable && nicknameInput.value) {
            updateMessage += `\n- 새 닉네임: ${nicknameInput.value}`;
        }
        if (newPassword) {
            updateMessage += `\n- 새 비밀번호 설정 완료`;
        }
        alert(updateMessage);
        closeModal(editProfileModal);
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
        // 드래그를 종료하는 로직을 하나의 함수로 통합 (mouseup, mouseleave 공통 사용)
        const endDrag = (e) => {
            if (!isMouseDown) return;
            isMouseDown = false;
            carouselWrapper.classList.remove('dragging');
            // 애니메이션 효과를 다시 켭니다.
            carousel.style.transition = 'transform 0.3s ease-in-out';

            const touchEndX = e.clientX;
            const swipeDistance = touchEndX - touchStartX;

            // 드래그 거리를 판정하여 페이지 이동 또는 원위치를 결정합니다.
            if (swipeDistance < -50) { // 왼쪽으로 충분히 스와이프
                goToPage(currentPageIndex + 1);
            } else if (swipeDistance > 50) { // 오른쪽으로 충분히 스와이프
                goToPage(currentPageIndex - 1);
            } else {
                // 드래그 거리가 짧으면 원래 페이지로 부드럽게 복귀
                goToPage(currentPageIndex, true);
            }
        };

        // 1. 마우스를 누르기 시작할 때
        carouselWrapper.addEventListener('mousedown', (e) => {
            isMouseDown = true;
            touchStartX = e.clientX;
            carouselWrapper.classList.add('dragging');
            e.preventDefault(); // 브라우저 기본 드래그 동작 방지
        });

        // 2. 마우스를 움직일 때 (실시간 드래그 효과)
        carouselWrapper.addEventListener('mousemove', (e) => {
            if (!isMouseDown) return;
            const currentX = e.clientX;
            const distance = currentX - touchStartX;

            const baseOffset = -currentPageIndex * 100;
            carousel.style.transition = 'none'; // 실시간 이동 중에는 애니메이션 효과를 끔
            carousel.style.transform = `translateX(calc(${baseOffset}% + ${distance}px))`;
        });

        // 3. 마우스 버튼을 뗄 때 드래그 종료
        carouselWrapper.addEventListener('mouseup', endDrag);

        // 4. 마우스가 영역 밖으로 나갔을 때도 드래그 종료로 처리 (오류 수정)
        carouselWrapper.addEventListener('mouseleave', endDrag);
    }

    if (followBtn) {
        followBtn.addEventListener('click', async (event) => {
            const button = event.currentTarget;
            const userId = button.dataset.userid;
            const isFollowing = button.classList.contains('unfollow');

            const url = isFollowing ? '/friend/unfollowing' : '/friend/addfollowing';
            const method = isFollowing ? 'DELETE' : 'POST';

            // CSRF 토큰 헤더 준비 (Spring Security 사용 시 필요)
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            const headers = {'Content-Type': 'application/json'};
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            try {
                const response = await fetch(url, {
                    method: method,
                    headers: headers,
                    body: JSON.stringify({userId: userId})
                });

                if (response.ok) {
                    // 성공 시 버튼 모양과 텍스트를 즉시 변경
                    if (isFollowing) {
                        button.classList.replace('unfollow', 'follow');
                        button.textContent = 'Follow';
                    } else {
                        button.classList.replace('follow', 'unfollow');
                        button.textContent = 'Following';
                    }
                    // (선택) 팔로워 수 실시간 변경이 필요하면 페이지를 새로고침 할 수도 있습니다.
                    // location.reload();
                } else {
                    alert('요청 처리 중 오류가 발생했습니다.');
                }
            } catch (error) {
                console.error('Follow error:', error);
            }
        });
    }

    if (isMyProfile && profileImageInput) {
        profileImageInput.addEventListener('change', async (event) => {
            const file = event.target.files[0];
            if (!file) {
                return; // 파일 선택을 취소한 경우
            }

            // 1. FormData 객체를 만들어 선택한 파일을 담습니다.
            const formData = new FormData();
            formData.append('profileImage', file); // Controller의 @RequestParam("profileImage")와 이름이 같아야 합니다.

            // CSRF 토큰 헤더 준비
            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
            const headers = {};
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            try {
                // 2. FormData를 body에 담아 /api/profile/image로 POST 요청을 보냅니다.
                const response = await fetch('/api/profile/image', {
                    method: 'POST',
                    headers: headers, // FormData 전송 시 Content-Type은 브라우저가 자동으로 설정하므로 넣지 않습니다.
                    body: formData
                });

                if (response.ok) {
                    const result = await response.json();

                    // 3. 성공 시, 응답으로 받은 새 이미지 URL을 <img> 태그의 src에 적용합니다.
                    // 캐시 문제를 피하기 위해 타임스탬프를 추가합니다.
                    profileImage.src = result.imageUrl + '?t=' + new Date().getTime();

                    alert('프로필 이미지가 성공적으로 변경되었습니다.');
                } else {
                    // 서버에서 오류 응답이 온 경우
                    const errorResult = await response.json();
                    alert('이미지 변경에 실패했습니다: ' + (errorResult.message || '서버 오류'));
                }
            } catch (error) {
                console.error('Error uploading profile image:', error);
                alert('이미지 업로드 중 오류가 발생했습니다.');
            }
        });
    }

    // --- 6. 초기 실행 ---
    loadMyPark();
    loadPosts(initialTab, 0);
}

