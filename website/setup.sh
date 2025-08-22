#!/bin/bash

sudo apt-get install apache2 php php-mysql composer

HERE=$(realpath $(dirname $0))

cp $HERE/*.jpg $HERE/*.png $HERE/*.html $HERE/*.php /var/www/html/

pushd /var/www/html

composer require nesbot/carbon
composer require cocur/human-date

popd
