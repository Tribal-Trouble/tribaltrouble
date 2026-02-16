<?php
$id = $_GET['gid'];
$offset = $_GET['offset'];
$size = $_GET['size'];
$name = "/var/games/$id";
$file = fopen($name, 'r');
if ($file === false) {
    die('Failed to open file');
}
fseek($file, $offset, SEEK_SET);
$data = fread($file, $size);
echo $data;
fclose($file);
