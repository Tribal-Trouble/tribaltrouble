<?php

require_once __DIR__ . '/vendor/autoload.php';

use Carbon\Carbon;
use Cocur\HumanDate\HumanDate;

$config = require '/var/creds/creds.php';
$db = $config['database'];

$conn = mysqli_connect($db['host'], $db['user'], $db['password'], $db['dbname']);
if (!$conn) {
    die('Connection failed: ' . mysqli_connect_error());
}

$n = $_GET['name'];
$offset = (int)$_GET['offset'];

$sql = "select * from games where id in (select game_id from game_players where nick='$n') and status='completed' order by time_start desc limit 10 offset $offset";

$response = array();
$result = mysqli_query($conn, $sql);
while ($row = mysqli_fetch_assoc($result)) {
    $humanDate = new HumanDate();
    $time = $row['time_stop'];
    $last = $humanDate->transform($time);
    if ($last == 'Today') {
        $date = Carbon::parse($time);
        $diff = $date->diffForHumans();
        $last = "$last $diff";
    }

    $row['htime'] = $last;

    $id = $row['id'];
    $row['watchable'] = file_exists("/var/games/$id");

    $sql = "SELECT * from game_players where game_id=$id";
    $players = mysqli_query($conn, $sql);
    $players_response = array();
    while ($player = mysqli_fetch_assoc($players)) {
        array_push($players_response, $player);
    }

    $row['players'] = $players_response;

    array_push($response, $row);
}

header("Content-Type: application/json");
echo json_encode($response);

mysqli_close($conn);

?>

