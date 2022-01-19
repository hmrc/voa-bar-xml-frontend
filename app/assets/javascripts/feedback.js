;(function ($, global) {
    var VoaMessages = {
        textLabel: function (t) {
            var text;
            text = {
                labelBetaFeedback: 'How satisfied are you with this service?',
                labelBetaFeedbackImprove: 'How can we improve this form?',
                labelBetaFeedbackDontInclude: 'Don’t include any personal or financial information.',
                labelBetaFeedbackButton: 'Send feedback',
                labelBetaFeedback5: 'Very satisfied',
                labelBetaFeedback4: 'Satisfied',
                labelBetaFeedback3: 'Neither satisfied or dissatisfied',
                labelBetaFeedback2: 'Dissatisfied',
                labelBetaFeedback1: 'Very dissatisfied',
                labelCommentLimit: 'Limit is 2000 characters',
                labelHelpName: 'Name',
                labelHelpEmail: 'Email',
                labelHelpAction: 'What were you doing?',
                labelHelpError: 'What do you need help with?',
                errorHelpName: 'Please provide your name',
                errorHelpEmail: 'Please provide your email address.',
                errorHelpAction: 'Please enter details of what you were doing.',
                errorHelpError: 'Please enter details of what went wrong.',
                buttonHelpSend: 'Send',
                labelFeedbackComments: 'Comments',
                errorHelpEmailInvalid: 'Enter a valid email address.',
                errorHelpCommentsRequired: 'This field is required',
                errorHelpRequired: 'Tell us what you think of the service.'
            };
            return text[t];
        }
    };

    var VoaFeedback = {};
    VoaFeedback.feedbackOverrides = function(){
        //remove if js
        $('.deleteifjs').remove();
        //feedback overrides
        $('.label--inlineRadio--overhead').addClass('block-label').removeClass('label--inlineRadio--overhead');
        $('.input--fullwidth').addClass('form-control').removeClass('input--fullwidth');
        $('#feedback-form fieldset fieldset legend').text(VoaMessages.textLabel('labelBetaFeedback'));
        $('#feedback-form fieldset small').text(VoaMessages.textLabel('labelBetaFeedback'));
        $('.form--feedback fieldset small').text(VoaMessages.textLabel('labelCommentLimit'));
        $('<strong class="feedbackLabel">'+VoaMessages.textLabel('labelBetaFeedbackImprove')+'</strong>').insertBefore($('label[for="feedback-name"]'));
        $('<p class="feedbackLabelFooter">'+VoaMessages.textLabel('labelBetaFeedbackDontInclude')+'</p>').insertAfter($('[for="feedback-comments"]'));
        $('#feedback-form [type="submit"]').text(VoaMessages.textLabel('labelBetaFeedbackButton'));
        $('.form--feedback label[for="report-name"], .form--feedback label[for="feedback-name"] span:not(".form-field--error")').text(VoaMessages.textLabel('labelHelpName'));
        $('.form--feedback label[for="report-email"], .form--feedback label[for="feedback-email"] span:not(".form-field--error")').text(VoaMessages.textLabel('labelHelpEmail'));
        $('.form--feedback label[for="report-action"]').text(VoaMessages.textLabel('labelHelpAction'));
        $('.form--feedback label[for="report-error"]').text(VoaMessages.textLabel('labelHelpError'));
        $('.form--feedback input[id="report-name"]').attr('data-msg-required', VoaMessages.textLabel('errorHelpName'));
        $('.form--feedback input[id="report-email"]').attr('data-msg-required', VoaMessages.textLabel('errorHelpEmail'));
        $('.form--feedback input[id="report-action"]').attr('data-msg-required', VoaMessages.textLabel('errorHelpAction'));
        $('.form--feedback input[id="report-error"]').attr('data-msg-required', VoaMessages.textLabel('errorHelpError'));
        $('.form--feedback input[id="report-error"]').attr('data-msg-required', VoaMessages.textLabel('errorHelpError'));
        $('.form--feedback button[type="submit"]').text(VoaMessages.textLabel('buttonHelpSend'));
        $('.form--feedback label[for="feedback-name"] .error-notification').text(VoaMessages.textLabel('errorHelpName'));
        $('.form--feedback label[for="feedback-email"] .error-notification').text(VoaMessages.textLabel('errorHelpEmailInvalid'));
        $('.form--feedback label[for="feedback-comments"] .error-notification').text(VoaMessages.textLabel('errorHelpCommentsRequired'));
        $('.form--feedback fieldset.form-field--error span.error-notification').text(VoaMessages.textLabel('errorHelpRequired'));

        //removed email and name from form, comments optional and bigger
        var comments = $('#feedback-form [for="feedback-comments"]');
        var commentsHtml = $('#feedback-form [for="feedback-comments"]').html();
        var feedbackHelpToRemove = $('.form-title');
        if(feedbackHelpToRemove.siblings()) {
            feedbackHelpToRemove.siblings().first().remove();
        }
        feedbackHelpToRemove.remove()
        if (commentsHtml) {
            $('#feedback-form [for="feedback-comments"]').html(commentsHtml.replace('Comments', 'Comments (Optional)'));
            $('#feedback-form [for="feedback-comments"] p').remove();
        }
        $('#feedback-form [for="feedback-email"]').css('display', 'none');
        $('#feedback-form [for="feedback-name"]').css('display', 'none');
        $('#feedback-form [name="feedback-name"]').val('Anonymous user');
        $('#feedback-form [name="feedback-email"]').val('anonymous@anonymous.com');
        //

        //vacate form
        $('.vacated-form [for="report-name"]').text(VoaMessages.textLabel('vacateFormName'));
        $('.vacated-form [for="report-email"]').text(VoaMessages.textLabel('vacateFormEmail'));
        $('.vacated-form [for="report-error"]').text(VoaMessages.textLabel('vacateFormGiveDetails'));

        $('.vacated-form [name="report-action"]').closest('div').show();
        $('.vacated-form label[for="report-action"]').addClass('govuk-visually-hidden');
        $('.vacated-form [name="report-action"]').val(VoaMessages.textLabel('labelWhatWereYouDoing')).attr('hidden', '');


        var needle  =  $('.form--feedback label[for="feedback-comments"]').html();
        if(needle){
            $('.form--feedback label[for="feedback-comments"]').html(needle.replace(/Comments/g, VoaMessages.textLabel('labelFeedbackComments')));

        }
    };

    VoaFeedback.toggleHelp = function(){
        $('.form-help-toggle').click(function(e){
            e.preventDefault();
            $('#helpForm').toggle();
        });
    };

    VoaFeedback.helpForm = function() {
        $('#helpForm').on('submit', 'form', function(event) {
            event.preventDefault();
            $('#report-submit').prop('disabled', true);
            $.ajax({
                type: 'POST',
                url:  $(this).attr('action'),
                data: $(this).serialize(),
                success: function(msg) {
                    console.log('Got success response: ' + msg);
                    $('.contact-form-copy').addClass('hidden');
                    $('.feedback-thankyou').removeClass('hidden');
                    $('.form--feedback').addClass('hidden');
                    $('#helpFormWrapper').addClass('hidden');
                },
                error: function(error) {
                    $('#helpFormWrapper').html('<p>'+error.responseText+'</p>');
                    $('.contact-form-copy').addClass('hidden');
                    $('.feedback-thankyou').removeClass('hidden');
                    $('.form--feedback').addClass('hidden');
                }
            });

        });
    };

    window.VoaFeedback = VoaFeedback;
})(jQuery, window);
