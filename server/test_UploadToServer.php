<?php

    try {
        $file_path = "/var/services/web/test_temp/";
        if (!file_exists($file_path)) {
        	mkdir($file_path, 0777, true);
    	}
        $file_path = $file_path.basename($_FILES['uploaded_file']['name']);
        $filename = $_FILES["uploaded_file"]["name"];

		switch ($_FILES['uploaded_file']['error']) {
        case UPLOAD_ERR_OK:
            break;
        case UPLOAD_ERR_NO_FILE:
            throw new RuntimeException('No file sent.');
        case UPLOAD_ERR_INI_SIZE:
        case UPLOAD_ERR_FORM_SIZE:
            throw new RuntimeException('Exceeded filesize limit.');
        default:
            break;
		}
	} catch (RuntimeException $e) {
		file_put_contents('php://stderr', print_r($e->getMessage(), TRUE));
        echo $e->getMessage();
	}
    
    if($filename == "end")
    {
    	//call machine learning program
        exec('python3 test.py -d test_temp &> test_dump', $display, $return_out); 
		file_put_contents('test_dump', $display);
        
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
   	    echo "Not uploaded because of error #".$_FILES["file"]["error"];
    }
 ?>
