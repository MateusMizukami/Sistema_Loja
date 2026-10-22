(function () {
    'use strict';

    var estado = document.getElementById('estado');
    if (!estado) { return; }

    var pedido = estado.getAttribute('data-pedido');
    var inicial = estado.getAttribute('data-status');

    // Botão de copiar o código PIX
    var botao = document.getElementById('botao-copiar');
    var campo = document.getElementById('copia');
    if (botao && campo) {
        botao.addEventListener('click', function () {
            campo.select();
            var copiado = function () { botao.textContent = 'Código copiado!'; };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(campo.value).then(copiado, function () {
                    document.execCommand('copy');
                    copiado();
                });
            } else {
                document.execCommand('copy');
                copiado();
            }
        });
    }

    // Enquanto estiver pendente, confere o status a cada 5 segundos e recarrega quando mudar
    if (inicial !== 'PENDENTE') { return; }

    setInterval(function () {
        fetch('/pedidos/' + pedido + '/pagamento/status', {
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        })
            .then(function (resposta) { return resposta.ok ? resposta.json() : null; })
            .then(function (dados) {
                if (dados && dados.pagamento !== 'PENDENTE') {
                    estado.textContent = 'A situação do pagamento mudou. Atualizando a página.';
                    window.location.reload();
                }
            })
            .catch(function () {});
    }, 5000);
})();
