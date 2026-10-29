/*
 * Fills in the server address (from /config.js, rendered by nginx from .env) and the
 * download card (from /downloads/release.json, written by :client-fx:packageZip).
 */
(() => {
    'use strict';

    const config = window.BOMBERMAN_CONFIG || {};
    // An empty host means "the game server shares the website's hostname".
    const host = String(config.host || '').trim() || window.location.hostname || 'localhost';
    const port = String(config.port || '').trim() || '8081';
    const copyValues = { host, port, address: `${host}:${port}`, sha256: '' };

    const all = (selector) => document.querySelectorAll(selector);
    const setText = (selector, text) => all(selector).forEach((element) => { element.textContent = text; });

    setText('[data-server-host]', host);
    setText('[data-server-port]', port);
    setText('[data-server-address]', copyValues.address);

    const repoUrl = String(config.repoUrl || '').trim();
    if (/^https?:\/\//i.test(repoUrl)) {
        all('[data-repo-link]').forEach((link) => {
            link.href = repoUrl;
            link.hidden = false;
        });
    }

    /* ---------- Toast ---------- */
    const toast = document.querySelector('[data-toast]');
    let toastTimer = 0;

    function showToast(message, tone = 'success') {
        if (!toast) {
            return;
        }
        toast.textContent = message;
        toast.dataset.tone = tone;
        toast.classList.add('is-visible');
        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(() => toast.classList.remove('is-visible'), 2800);
    }

    /* ---------- Copy buttons ---------- */
    // navigator.clipboard only exists on HTTPS/localhost; the site may be served over
    // plain HTTP on a home DDNS name, so fall back to a temporary textarea.
    async function copyText(text) {
        if (navigator.clipboard && window.isSecureContext) {
            try {
                await navigator.clipboard.writeText(text);
                return true;
            } catch (ignored) {
                // Permission denied: try the fallback below.
            }
        }
        const previousFocus = document.activeElement;
        const area = document.createElement('textarea');
        area.value = text;
        area.setAttribute('readonly', '');
        area.style.position = 'fixed';
        area.style.top = '0';
        area.style.left = '0';
        area.style.opacity = '0';
        document.body.append(area);
        area.select();
        let copied = false;
        try {
            copied = document.execCommand('copy');
        } catch (ignored) {
            copied = false;
        }
        area.remove();
        if (previousFocus instanceof HTMLElement) {
            previousFocus.focus({ preventScroll: true });
        }
        return copied;
    }

    document.addEventListener('click', async (event) => {
        const button = event.target.closest('[data-copy]');
        if (!button) {
            return;
        }
        const value = copyValues[button.dataset.copy];
        if (!value) {
            return;
        }
        if (await copyText(value)) {
            if (!button.dataset.label) {
                button.dataset.label = button.textContent;
            }
            button.textContent = 'Đã sao chép';
            window.setTimeout(() => { button.textContent = button.dataset.label; }, 1600);
            showToast(`Đã sao chép: ${value}`);
        } else {
            showToast('Không sao chép được. Hãy bôi đen và sao chép thủ công.', 'error');
        }
    });

    /* ---------- Download card ---------- */
    const downloadLinks = all('[data-download-link]');
    const status = document.querySelector('[data-release-status]');

    function formatSize(bytes) {
        const megabytes = bytes / (1024 * 1024);
        return `${megabytes.toLocaleString('vi-VN', { maximumFractionDigits: 1 })} MB`;
    }

    function isValidRelease(release) {
        return release
            && typeof release.file === 'string' && /^[\w.-]+\.zip$/.test(release.file)
            && typeof release.sha256 === 'string' && /^[0-9a-f]{64}$/i.test(release.sha256)
            && Number.isFinite(release.sizeBytes) && release.sizeBytes > 0;
    }

    function showRelease(release) {
        const fileUrl = `/downloads/${encodeURIComponent(release.file)}`;
        const version = String(release.version || '').trim() || 'mới nhất';
        const size = formatSize(release.sizeBytes);

        downloadLinks.forEach((link) => {
            link.href = fileUrl;
            link.setAttribute('download', release.file);
            link.removeAttribute('aria-disabled');
            link.dataset.ready = 'true';
        });
        all('[data-checksum-link]').forEach((link) => {
            link.href = `${fileUrl}.sha256`;
            link.setAttribute('download', `${release.file}.sha256`);
        });

        setText('[data-release-field="version"]', version);
        setText('[data-release-field="size"]', `${size} (file ZIP)`);
        setText('[data-release-field="file"]', release.file);
        setText('[data-release-field="sha256"]', release.sha256);
        setText('[data-release-file]', release.file);
        setText('[data-release-summary]', `Phiên bản ${version} · Windows 10/11 64-bit · ${size}`);
        setText('[data-checksum-command]', `Get-FileHash .\\${release.file} -Algorithm SHA256`);

        copyValues.sha256 = release.sha256;
        all('[data-copy="sha256"]').forEach((button) => { button.disabled = false; });

        if (status) {
            const builtAt = new Date(release.builtAt);
            status.textContent = Number.isNaN(builtAt.getTime())
                ? 'Bản tải đã sẵn sàng.'
                : `Đóng gói ngày ${builtAt.toLocaleDateString('vi-VN')}.`;
        }
    }

    function showMissingRelease() {
        // Hero and step links keep pointing at #tai-game, where this message is shown.
        all('.download-button').forEach((link) => link.setAttribute('aria-disabled', 'true'));
        if (status) {
            status.textContent = 'Server này chưa có bản tải. Vui lòng quay lại sau.';
            status.dataset.tone = 'error';
        }
    }

    document.addEventListener('click', (event) => {
        const link = event.target.closest('[data-download-link]');
        if (!link) {
            return;
        }
        if (link.getAttribute('aria-disabled') === 'true') {
            event.preventDefault();
            return;
        }
        if (link.dataset.ready === 'true') {
            showToast('Đang tải… Sau đó làm theo phần Hướng dẫn cài đặt.');
        }
    });

    fetch('/downloads/release.json', { cache: 'no-store' })
        .then((response) => {
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            return response.json();
        })
        .then((release) => {
            if (!isValidRelease(release)) {
                throw new Error('Invalid release.json');
            }
            showRelease(release);
        })
        .catch(showMissingRelease);
})();
