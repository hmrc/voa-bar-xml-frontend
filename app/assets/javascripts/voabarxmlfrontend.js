$(document).ready(function() {

  // =====================================================
  // Initialise show-hide-content
  // Toggles additional content based on radio/checkbox input state
  // =====================================================
  var showHideContent, mediaQueryList;
  showHideContent = new GOVUK.ShowHideContent()
  showHideContent.init()

  // =====================================================
  // Handle number inputs
  // =====================================================
    numberInputs();

  // =====================================================
  // Back link mimics browser back functionality
  // =====================================================
  $('#back-link').on('click', function(e){
    e.preventDefault();
    window.history.back();
  })

  // =====================================================
  // Adds data-focuses attribute to all containers of inputs listed in an error summary
  // This allows validatorFocus to bring viewport to correct scroll point
  // =====================================================
      function assignFocus () {
          var counter = 0;
          $('.error-summary-list a').each(function(){
              var linkhash = $(this).attr("href").split('#')[1];
              $('#' + linkhash).parents('.form-field, .form-group').first().attr('id', 'f-' + counter);
              $(this).attr('data-focuses', 'f-' + counter);
              counter++;
          });
      }
      assignFocus();

      function beforePrintCall(){
          if($('.no-details').length > 0){
              // store current focussed element to return focus to later
              var fe = document.activeElement;
              // store scroll position
              var scrollPos = window.pageYOffset;
              $('details').not('.open').each(function(){
                  $(this).addClass('print--open');
                  $(this).find('summary').trigger('click');
              });
              // blur focus off current element in case original cannot take focus back
              $(document.activeElement).blur();
              // return focus if possible
              $(fe).focus();
              // return to scroll pos
              window.scrollTo(0,scrollPos);
          } else {
              $('details').attr("open","open").addClass('print--open');
          }
          $('details.print--open').find('summary').addClass('heading-medium');
      }

      function afterPrintCall(){
          $('details.print--open').find('summary').removeClass('heading-medium');
          if($('.no-details').length > 0){
              // store current focussed element to return focus to later
              var fe = document.activeElement;
              // store scroll position
              var scrollPos = window.pageYOffset;
              $('details.print--open').each(function(){
                  $(this).removeClass('print--open');
                  $(this).find('summary').trigger('click');
              });
              // blur focus off current element in case original cannot take focus back
              $(document.activeElement).blur();
              // return focus if possible
              $(fe).focus();
              // return to scroll pos
              window.scrollTo(0,scrollPos);
          } else {
              $('details.print--open').removeAttr("open").removeClass('print--open');
          }
      }

      //Chrome
      if(typeof window.matchMedia != 'undefined'){
          mediaQueryList = window.matchMedia('print');
          mediaQueryList.addListener(function(mql) {
              if (mql.matches) {
                  beforePrintCall();
              };
              if (!mql.matches) {
                  afterPrintCall();
              };
          });
      }

      //Firefox and IE (above does not work)
      window.onbeforeprint = function(){
          beforePrintCall();
      }
      window.onafterprint = function(){
          afterPrintCall();
      }
  });


  function numberInputs() {
      // =====================================================
      // Set currency fields to number inputs on touch devices
      // this ensures on-screen keyboards display the correct style
      // don't do this for FF as it has issues with trailing zeroes
      // =====================================================
      if($('html.touchevents').length > 0 && window.navigator.userAgent.indexOf("Firefox") == -1){
          $('[data-type="currency"] > input[type="text"], [data-type="percentage"] > input[type="text"]').each(function(){
            $(this).attr('type', 'number');
            $(this).attr('step', 'any'); 
            $(this).attr('min', '0');
          });
      }

      // =====================================================
      // Disable mouse wheel and arrow keys (38,40) for number inputs to prevent mis-entry
      // also disable commas (188) as they will silently invalidate entry on Safari 10.0.3 and IE11
      // =====================================================
      $("form").on("focus", "input[type=number]", function(e) {
          $(this).on('wheel', function(e) {
              e.preventDefault();
          });
      });
      $("form").on("blur", "input[type=number]", function(e) {
          $(this).off('wheel');
      });
      $("form").on("keydown", "input[type=number]", function(e) {
          if ( e.which == 38 || e.which == 40 || e.which == 188 )
              e.preventDefault();
      });
      // =====================================================
      // Upscan upload
      // =====================================================
      $("#councilTaxUploadForm").submit(function(e){
        e.preventDefault();
        const fileLength = $("#file")[0].files.length;
        if(fileLength === 0){
            window.location = $("#councilTaxUploadEmptyFileError").val();
        } else {
            var councilTaxUploadForm = this;
            function submitError(error, jqXHR){
                var payload = {
                    code: error,
                    values: [],
                    errorDetail: jqXHR
                };
                $.ajax({
                      url: $("#councilTaxUploadReportError").val(),
                      type: "POST",
                      data: JSON.stringify(payload),
                      contentType: 'application/json'
                }).complete(function(){
                    window.location = $("#councilTaxUploadFormRedirect").val();
                });
            };

            function fileUpload(form){
                $.ajax({
                      url: form.action,
                      type: "POST",
                      data: new FormData(form),
                      processData: false,
                      contentType: false,
                      crossDomain: true
                }).error(function(jqXHR, textStatus, errorThrown ){
                    submitError("4000", jqXHR)
                }).done(function(){
                      refreshPage();
                });
            };

            $.ajax({
                url: $("#councilTaxUploadPrepareUpload").val(),
                method: "POST",
                contentType: "application/json",
                data: '{}',
                xhrFields: {
                    withCredentials: true
                }
            }).error(function(jqXHR, textStatus, errorThrown ){
                submitError("5000", jqXHR)
            }).done(function(){
                fileUpload(councilTaxUploadForm);
                // Disable UI
                $("#file").before(
                "<div id=\"processing\" aria-live=\"polite\" class=\"govuk-!-margin-bottom-5\">" +
                "<h2 class=\"govuk-heading-m\">We are checking your file, please wait</h2>" +
                "<div><div class=\"ccms-loader\"></div></div></div>"
                )
                $("#file").attr('disabled', 'disabled')
                $("#submit").addClass('govuk-button--disabled')
            });
        }

      });
      // =====================================================
      // CouncilTaxUpload Refresh status page
      // =====================================================
      function refreshPage(){
            var refreshUrl = $("#councilTaxUploadRefreshUrl").val();
            if (refreshUrl) {
                window.refreshIntervalId = setInterval(function () {
                    console.debug("scheduling ajax call, refreshUrl", refreshUrl)

                    $.getJSON(refreshUrl, function (data, textStatus, jqXhr) {
                        if (jqXhr.status === 200) {
                            if ($("#reportStatus").val() !== data.status) {
                                console.debug("status changed, updating page", data.status);

                                if (data.status === "Failed" || data.status === "Done") {
                                    console.debug("Reached final status, removing refresh", status)
                                    clearInterval(window.refreshIntervalId);
                                    window.location = $("#councilTaxUploadFormRedirect").val();
                                    console.debug("interval cleared");
                                }
                            } else {
                                console.debug("status didn't change, we not updating anything");
                            }
                        } else {
                            console.log("Something went wrong", jqXhr);
                        }
                    });
                }, 3000);
                console.log("intervalRefreshScheduled, id: ", window.refreshIntervalId);
            }

      }

    // =====================================================
    // WebForm Confirmation Refresh status page
    // =====================================================
    var refreshUrl = $("#refreshUrl").val();
    if (refreshUrl) {
        window.refreshIntervalId = setInterval(function () {
            console.debug("scheduling ajax call, refreshUrl", refreshUrl)
            $.getJSON(refreshUrl, function (data, textStatus, jqXhr) {
                if (jqXhr.status === 200) {
                    if ($("#reportStatus").val() !== data.status) {
                        console.debug("status changed, updating page", data.status);

                        //Modify DOM
                        $("#status").html($(data.statusPanel));
                        $("#confirmationDetailPanel").html($(data.detailPanel));
                        $("#reportStatus").val(data.status);

                        console.debug("page updated");
                        if (data.status === "Failed" || data.status === "Submitted" || data.status === "Done") {
                            console.debug("Reached final status, removing refresh", status)
                            clearInterval(window.refreshIntervalId);
                            console.debug("interval cleared");
                        }
                    } else {
                        console.debug("status didn't change, we not updating anything");
                    }
                } else {
                    console.log("Something went wrong", jqXhr);
                }
            });
        }, 3000);
        console.log("intervalRefreshScheduled, id: ", window.refreshIntervalId);
    }


      //feedback.js
      window.VoaFeedback.feedbackOverrides();
      window.VoaFeedback.toggleHelp();
      window.VoaFeedback.helpForm();
  }
