// src/main/resources/static/js/forum-area-search.js
export function initForumAreaSearch() {
    const searchInput = $('#location-search');
    const resultsList = $('#search-results');
    let availableLocations = []; // 지역 목록을 저장할 빈 배열

    // 1. 페이지 로드 시 백엔드에서 지역 목록을 가져오는 함수
    function fetchAvailableLocations() {
        $.ajax({
            url: '/forum/areas',
            type: 'GET',
            dataType: 'json',
            success: function (result) {
                if (result.status === '000') {
                    availableLocations = result.data; // 가져온 데이터를 배열에 저장
                } else {
                    console.error('지역 목록을 불러오는 데 실패했습니다:', result.message);
                    alert('지역 목록을 불러오는 데 실패했습니다. 다시 시도해주세요.');
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.error('네트워크 오류:', textStatus, errorThrown);
                alert('네트워크 오류로 지역 목록을 가져올 수 없습니다.');
            }
        });
    }

    // 2. 페이지 로드 시 함수 실행
    fetchAvailableLocations();

    // ----------------------------------------------------
    // 한글 자모 검색을 위한 로직
    // ----------------------------------------------------
    const CHO = ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'];
    const JUNG = ['ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ', 'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'];
    const JONG = ['', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ', 'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'];

    function getJamo(char) {
        const code = char.charCodeAt(0);
        const jamoCode = code - 0xAC00;

        if (jamoCode >= 0 && jamoCode <= 11172) {
            const choIndex = Math.floor(jamoCode / 588);
            const jungIndex = Math.floor((jamoCode % 588) / 28);
            const jongIndex = jamoCode % 28;

            return CHO[choIndex] + JUNG[jungIndex] + JONG[jongIndex];
        }
        return char;
    }

    function isMatch(text, query) {
        const textJamo = text.split('').map(getJamo).join('').toLowerCase();
        const queryJamo = query.split('').map(getJamo).join('').toLowerCase();

        return textJamo.includes(queryJamo);
    }

    // 3. 입력창에 타이핑할 때마다 검색 결과를 업데이트
    searchInput.on('input', function () {
        const query = searchInput.val().trim();
        resultsList.empty();

        if (query.length > 0) {
            const filteredLocations = availableLocations.filter(area =>
                // isMatch 함수를 사용해 한글 자모 검색을 수행
                isMatch(area.address, query)
            );

            let str = '';
            filteredLocations.forEach(area => {
                str += `<li data-location-id="${area.id}">${area.address}</li>`;
            });
            resultsList.html(str);
        }
    });

    // 4. 검색 결과 항목을 클릭했을 때의 동작
    resultsList.on('click', 'li', function () {
        const selectedLocation = $(this).text();
        const locationId = $(this).data('location-id');

        $.ajax({
            url: '/forum/searchArea',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({location: selectedLocation}),
            success: function (result) {
                if (result.status === '000') {
                    alert('지역이 성공적으로 설정되었습니다: ' + selectedLocation);
                    window.location.href = result.data;
                } else {
                    alert('작성 실패: ' + (result.message || '서버 오류'));
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.error('API 요청 오류:', textStatus, errorThrown);
                alert('네트워크 오류가 발생했습니다.');
            }
        });
    });
}