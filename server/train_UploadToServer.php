<?php

    $file_path = "/var/services/web/train_temp/";
    if (!file_exists($file_path)) {
    	mkdir($file_path, 0777, true);
	}
    $file_path = $file_path.basename($_FILES['uploaded_file']['name']);
    $filename = $_FILES["uploaded_file"]["name"];
    
    if($filename == "end")
    {
    	//call machine learning program
        $output = system('python3 train.py -d train_temp &> train_dump', $return_out); 
        
	    //--------------- remove files in folder "train"---------------
        $files = glob('/var/services/web/train_temp/*'); //get all file names
		foreach($files as $file){ // iterate files
			if(is_file($file))
			{
				unlink($file); // delete file
			}
		}
	    //--------------- remove files in folder "train"---------------

	    echo $return_out;
    }
    else if(move_uploaded_file($_FILES['uploaded_file']['tmp_name'], $file_path)) {
        echo "success";
    } 
    else{
       echo "fail"; 
    }

    system("./AppFinder.o");
 ?>
