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

    // 상태 관리 변수
    let currentTab = 'find';
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
                ? `/api/v1/finds/my-posts?page=${page}&size=4`
                : `/api/v1/forums/my-posts?page=${page}&size=4`;

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
            const response = await fetch('/api/v1/users/me/park');
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

        tabFind.classList.toggle('active', tab === 'find');
        tabForum.classList.toggle('active', tab === 'forum');

        currentPageIndex = 0; // 탭 전환 시 첫 페이지로

        if (state[tab].content.length === 0) {
            loadPosts(tab, 0); // 데이터가 없으면 첫 페이지 로드
        } else {
            renderCarousel(); // 데이터가 있으면 바로 렌더링
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
        // 1. 마우스를 누르기 시작할 때
        carouselWrapper.addEventListener('mousedown', (e) => {
            isMouseDown = true;
            touchStartX = e.clientX; // 마우스의 시작 X좌표 저장
            carouselWrapper.classList.add('dragging'); // '잡고있는' 커서 모양으로 변경

            // 이미지나 링크를 드래그하는 브라우저 기본 동작 방지
            e.preventDefault();
        });

        // 2. 마우스를 움직일 때 (실시간으로 살짝 움직이는 효과)
        carouselWrapper.addEventListener('mousemove', (e) => {
            if (!isMouseDown) return;
            const currentX = e.clientX;
            const distance = currentX - touchStartX;

            // 현재 페이지 위치에서 드래그한 거리만큼 살짝 움직여 보이게 함
            const baseOffset = -currentPageIndex * 100;
            carousel.style.transition = 'none'; // 실시간 이동 중에는 transition을 끔
            carousel.style.transform = `translateX(calc(${baseOffset}% + ${distance}px))`;
        });

        // 3. 마우스 버튼을 뗄 때 (스와이프 동작 실행)
        carouselWrapper.addEventListener('mouseup', (e) => {
            if (!isMouseDown) return;
            isMouseDown = false;
            carouselWrapper.classList.remove('dragging'); // 커서 모양 원래대로
            carousel.style.transition = 'transform 0.3s ease-in-out'; // transition 다시 켬

            const touchEndX = e.clientX;
            const swipeDistance = touchEndX - touchStartX;

            if (swipeDistance < -50) { // 왼쪽으로 충분히 드래그했으면
                goToPage(currentPageIndex + 1);
            } else if (swipeDistance > 50) { // 오른쪽으로 충분히 드래그했으면
                goToPage(currentPageIndex - 1);
            } else {
                // 충분히 드래그하지 않았으면 원래 페이지로 복귀
                goToPage(currentPageIndex, true);
            }
        });

        // 4. 마우스가 영역을 벗어났을 때 (드래그 취소)
        carouselWrapper.addEventListener('mouseleave', () => {
            if (!isMouseDown) return;
            isMouseDown = false;
            carouselWrapper.classList.remove('dragging');
            carousel.style.transition = 'transform 0.3s ease-in-out';
            goToPage(currentPageIndex, true); // 원래 페이지로 복귀
        });
    }

    // --- 6. 초기 실행 ---
    loadMyPark();
    loadPosts('find', 0); // 페이지 로드 시 Fin'd 탭의 첫 페이지 데이터를 불러옵니다.
}

