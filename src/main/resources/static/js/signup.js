document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('signup-form');
    const message = document.getElementById('message');

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        message.className = 'msg';
        message.textContent = '';

        const payload = {
            loginId: form.loginId.value.trim(),
            password: form.password.value,
            name: form.name.value.trim(),
            phone: form.phone.value.trim(),
            email: form.email.value.trim()
        };

        try {
            const response = await fetch('/api/auth/signup', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await response.json();
            if (!response.ok) {
                message.className = 'msg err';
                message.textContent = data.message || '회원가입에 실패했습니다.';
                return;
            }

            message.className = 'msg ok';
            message.textContent = data.message + ' 로그인 페이지로 이동합니다.';
            setTimeout(() => {
                location.href = '/login.html';
            }, 1000);
        } catch (e) {
            message.className = 'msg err';
            message.textContent = '네트워크 오류가 발생했습니다.';
        }
    });
});
