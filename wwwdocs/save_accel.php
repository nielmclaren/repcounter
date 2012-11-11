<?php

$data = file_get_contents('php://input');
file_put_contents('accel.tsv', $data);

