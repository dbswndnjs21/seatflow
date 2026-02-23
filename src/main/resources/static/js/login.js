document.addEventListener('DOMContentLoaded', () => {
    const message = document.getElementById('message');
    const params = new URLSearchParams(location.search);

    if (params.has('error')) {
        message.className = 'msg error';
        message.textContent = '아이디 또는 비밀번호가 올바르지 않습니다.';
    } else if (params.has('logout')) {
        message.className = 'msg ok';
        message.textContent = '로그아웃되었습니다.';
    }
});
