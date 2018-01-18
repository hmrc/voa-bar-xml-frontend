#!/bin/bash

echo "Applying migration Login"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /login               controllers.LoginController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /login               controllers.LoginController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeLogin                        controllers.LoginController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeLogin                        controllers.LoginController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "login.title = login" >> ../conf/messages.en
echo "login.heading = login" >> ../conf/messages.en
echo "login.checkYourAnswersLabel = login" >> ../conf/messages.en
echo "login.error.required = Enter login" >> ../conf/messages.en
echo "login.error.length = Login must be 100 characters or less" >> ../conf/messages.en

echo "Adding helper line into UserAnswers"
awk '/class/ {\
     print;\
     print "  def login: Option[String] = cacheMap.getEntry[String](LoginId.toString)";\
     print "";\
     next }1' ../app/utils/UserAnswers.scala > tmp && mv tmp ../app/utils/UserAnswers.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class/ {\
     print;\
     print "";\
     print "  def login: Option[AnswerRow] = userAnswers.login map {";\
     print "    x => AnswerRow(\"login.checkYourAnswersLabel\", s\"$x\", false, routes.LoginController.onPageLoad(CheckMode).url)";\
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Moving test files from generated-test/ to test/"
rsync -avm --include='*.scala' -f 'hide,! */' ../generated-test/ ../test/
rm -rf ../generated-test/

echo "Migration Login completed"
