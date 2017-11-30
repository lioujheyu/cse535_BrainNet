<?php

    $file_path = "/var/services/web/test_temp/";
    if (!file_exists($file_path)) {
    	mkdir($file_path, 0777, true);
	}
    $file_path = $file_path.basename($_FILES['uploaded_file']['name']);
    $filename = $_FILES["uploaded_file"]["name"];
    
    if($filename == "end")
    {
    	//call machine learning program
        $output = system('python3 test.py -d test_temp &> test_dump', $return_out); 
        
	    //--------------- remove files in folder "test"---------------
        $files = glob('/var/services/web/test_temp/*'); //get all file names
		foreach($files as $file){ // iterate files
			if(is_file($file))
			{
				unlink($file); // delete file
			}
		}
	    //--------------- remove files in folder "test"---------------

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
