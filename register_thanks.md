---
layout: page
title: "Snapcoin: Thanks for registering!"
---

Thanks!
=======

<i class="fa fa-exclamation-triangle"></i>
**Use this service at your own risk.** If you lose your snapcoin.net
account address you lose access to your funds.
{:.warning}

You should be getting a text from your snapcoin.net account address soon. Save this address to your contact list!

Here is a Bitcoin address associated with your account to send funds to:

<div style="width: 100%;">
<div style="margin: 0 auto; padding-top: 20px; width: 400px; height: 200px; background-color: white; border: 1px solid #ff00e2; text-align: center;">
<div id="qrcodetarget" style="padding: 10px; width: 128px; margin: 0 auto;"></div>
<h2 id="bitcoin_address">(This page only works with Javascript. Check the url though which has the address if registration was successful.)</h2>
</div>
</div>

<script src="qrcode.min.js"></script>

<script>

window.addEventListener('load', function() {
    var re = new RegExp("[13][a-km-zA-HJ-NP-Z0-9]{26,33}");
    var myParam = re.exec(location);
    if (myParam == null) {
        bitcoin_address.innerText = "There was an issue registering for an account.";
   } else {
        bitcoin_address.innerText = myParam[0];

        var qrcode = new QRCode("qrcodetarget", {
            text: "bitcoin:"+myParam[0],
            width: 128,
            height: 128,
            colorDark : "#000000",
            colorLight : "#ffffff",
            correctLevel : QRCode.CorrectLevel.L
        });
    }
});
</script>