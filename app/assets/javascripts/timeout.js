;(function ($, global) {

    var VoaTimeout = {};
    window.GOVUK = window.GOVUK || {}
    VoaTimeout.timeOutReminder = function(){
         var timeout = $('#signOutTimeout').val();
         var countdown = $('#signOutCountdown').val();
         var signOutUrl = $('#signOutUrl').val();
         console.log('Is it happening???' + window.GOVUK.timeoutDialog)
         console.log('Is it happening 2???' + JSON.stringify(global.GOVUK))
         if(window.GOVUK.timeoutDialog && signOutUrl) {
             window.GOVUK.timeoutDialog({
                 timeout: timeout,
                 countdown: countdown,
                 keepAliveUrl: window.location,
                 signOutUrl: signOutUrl
             });
         }
     };

    window.VoaTimeout = VoaTimeout;
})(jQuery, window);