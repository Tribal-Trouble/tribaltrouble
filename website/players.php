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

$offset = (int)$_GET['offset'];
$column = (int)$_GET['column'];

if ((int)$_GET['sort'] == 1) {
    $sort = "";
} else {
    $sort = "desc";
}

switch ($column) {
    case 0:
        $orderby = "wins $sort, (wins/losses) $sort, rating $sort";
        break;
    case 1:
        $orderby = "losses $sort";
        break;
    case 2:
    default:
        $orderby = "rating $sort, wins $sort, (wins/losses) $sort";
        break;
}

$sql = "select nick, wins, losses, rating from profiles where wins != 0 || losses != 0 order by $orderby limit 10 offset $offset";

$response = array();

$result = mysqli_query($conn, $sql);
while ($row = mysqli_fetch_assoc($result)) {
    $name = $row['nick'];
    $wins = $row['wins'];
    $losses = $row['losses'];
    $rating = $row['rating'];
    array_push($response, array("nick" => $name, "wins" => $wins, "losses" => $losses, "rating" => $rating));
}

header("Content-Type: application/json");
echo json_encode($response);

mysqli_close($conn);

?>

