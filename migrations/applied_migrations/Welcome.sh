#!/bin/bash

echo "Applying migration Welcome"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /online-or-upload                       controllers.WelcomeController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "welcome.title = welcome" >> ../conf/messages.en
echo "welcome.heading = welcome" >> ../conf/messages.en

echo "Moving test files from generated-test/ to test/"
rsync -avm --include='*.scala' -f 'hide,! */' ../generated-test/ ../test/
rm -rf ../generated-test/

echo "Migration Welcome completed"
