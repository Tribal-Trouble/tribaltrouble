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

$sql = "select * from games where status = 'completed'";

$result = mysqli_query($conn, $sql);
if (mysqli_num_rows($result) > 0) {
    $scores = array();
    while ($row = mysqli_fetch_assoc($result)) {
        $winner_team = $row['winner'];
        if ($winner_team == -1) {
            continue;
        }
        for ($i = 1; $i <= 8; $i++) {
            $team = $row["player${i}_team"];
            $name = $row["player${i}_name"];
            if ($team == $winner_team) {
                if (!isset($scores[$name])) {
                    $scores[$name] = 0;
                }
                $scores[$name]++;
            }
        }
    }
    asort($scores);
    echo "<table><tr><td class='tableheader'>Player</td><td class='tableheader'>Won games</td></tr>";
    foreach (array_reverse($scores) as $name => $score) {
        echo "<tr><td class='pname'>$name</td><td class='pscore'>$score</td></tr>";
    }
    echo '</table>';
}

$sql = "SELECT * from games where time_stop = (select MAX(time_stop) from games where status='completed')";
$result = mysqli_query($conn, $sql);
if (mysqli_num_rows($result) > 0) {
    $row = mysqli_fetch_assoc($result);
    $humanDate = new HumanDate();
    $time = $row['time_stop'];
    $last = $humanDate->transform($time);
    if ($last == 'Today') {
        $date = Carbon::parse($time);
        $diff = $date->diffForHumans();
        $last = "$last $diff";
    }
    echo "<br>Last game played was <i>$last</i>.";
    $winner = $row['winner'];
    $names = '';
    $losers = '';
    for ($i = 1; $i <= 8; $i++) {
        $team = $row["player${i}_team"];
        $name = $row["player${i}_name"];
        if ($team == $winner) {
            if ($names != '') {
                $names = "$names, $name";
            } else {
                $names = $name;
            }
        } elseif ($name != '') {
            if ($losers != '') {
                $losers = "$losers, $name";
            } else {
                $losers = $name;
            }
        }
    }
    if ($names != '') {
        echo " <b>$names</b> won";
        if ($losers != '') {
            echo " against <b>$losers</b>";
        }
        $id = $row['id'];
        if (file_exists("/var/games/$id")) {
            echo " <a href='watch.html#$id' target='new'>watch here</a>";
        }
        echo '</br>';
    }
}

echo '<br/>';
echo '<br/>';

$sql = "SELECT * from games where status='started'";
$result = mysqli_query($conn, $sql);
if (mysqli_num_rows($result) > 0) {
    while ($row = mysqli_fetch_assoc($result)) {
        $players = array();
        $name = $row['name'];
        $num_players = 0;
        for ($i = 1; $i <= 6; $i++) {
            $player = $row["player${i}_name"];
            if ($player != '') {
                $players[$num_players] = $player;
                $num_players++;
            }
        }
        $id = $row['id'];
        $watch_here = '';
        if (file_exists("/var/games/$id")) {
            $watch_here = "<a href='watch.html#$id' target='new'>Watch it live here!</a>";
        }
        if ($num_players == 2) {
            echo "<b>${players[0]}</b> and <b>${players[1]}</b> are playing <i>$name</i> now $watch_here";
        } elseif ($num_players > 2) {
            for ($i = 0; $i < $num_players - 1; $i++) {
                echo "<b>${players[$i]}</b>,";
            }
            echo " and <b>${players[$num_players - 1]}</b> are playing <i>$name</i> now $watch_here";
        }
    }
}

mysqli_close($conn);

?>

