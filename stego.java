import java.io.*;
import java.util.*;

class Steg
{

  /**
   * A constant to hold the number of bits per byte
   */
  private final int byteLength=8;

  /**
   * A constant to hold the number of bits used to store the size of the file extracted
   */
  protected final int sizeBitsLength=32;
  /**
   * A constant to hold the number of bits used to store the extension of the file extracted
   */
  protected final int extBitsLength=64;

  // constants to hold size in bits of hidden header, extension and file size
  protected final int headerBytes = 54;
  protected final int fSizeBits = 32;
  protected final int extBits = 64;

   /**
  Default constructor to create a steg object, doesn't do anything - so we actually don't need to declare it explicitly.
  */

  public Steg()
  {

  }

  /**
  A method for hiding a string in an uncompressed image file such as a .bmp or .png
  You can assume a .bmp will be used
  @param cover_filename - the filename of the cover image as a string
  @param payload - the string which should be hidden in the cover image.
  @return a string which either contains 'Fail' or the name of the stego image which has been
  written out as a result of the successful hiding operation.
  You can assume that the images are all in the same directory as the java files
  */

  public String hideString(String payload, String cover_filename)
  {

    String outfile = "stegoOutput.bmp";

    int paylen = payload.length();

    // load file into FileInputStream
    try {
      FileInputStream stream = new FileInputStream(cover_filename);
      FileOutputStream writer = new FileOutputStream(outfile);

      // write header bytes to file
      for (int i = 0; i < headerBytes; i++)
      {
        writer.write(stream.read());
      }

      // write length of string to output file
      for (int i = 0; i < sizeBitsLength; i++)
      {
        // bit to store in lsb
        // if bit % 2 != 0 then the bit to pass is 1 else is 0
        int bitToPass = paylen % 2;
        // get next byte of image file
        int byteToPass = stream.read();
        // swap lsb
        int bytesToWrite = swapLsb(bitToPass, byteToPass);
        // write to new file
        writer.write(bytesToWrite);

        paylen = paylen >> 1;
      }

      // loop through characters of payload
      for (int i = 0; i < payload.length(); i++)
      {
        // store byte of payload
        int payByte = payload.charAt(i);
        for (int j = 0; j < byteLength; j++)
        {
          // if bit % 2 != 0 then the bit to pass is 1 else is 0
          int bitToPass = payByte % 2;
          // byte of image to change = new byte of file
          int byteToPass = stream.read();

          int bytesToWrite = swapLsb(bitToPass, byteToPass);
          // write to file
          writer.write(bytesToWrite);
          // shift payload byte right one to get each bit
          payByte = payByte >> 1;
        }
      }

      // write the remaining bytes in file
      int toWrite = 0;
      while ((toWrite = stream.read()) != -1)
      {
        writer.write(toWrite);
      }

      //  return "stegoOutput.bmp";
      return outfile;

    }
    catch (FileNotFoundException e)
    {
      //error message as the user has selected a file which cannot be located
      System.err.println("The file " + cover_filename + " cannot be found, please" +
          " try again");
      return "Fail";
    }
    catch (IOException e)
    {
      System.err.println("Error");
      return "Fail";
    }

  }

  /**
  The extractString method should extract a string which has been hidden in the stegoimage
  @param the name of the stego image
  @return a string which contains either the message which has been extracted or 'Fail' which indicates the extraction
  was unsuccessful
  */
  public String extractString(String stego_image)
  {

    ArrayList<Integer> length = new ArrayList<Integer>();
    ArrayList<Integer> message = new ArrayList<Integer>();

    try
    {
      FileInputStream stream = new FileInputStream(stego_image);

      // skip header
      for (int i = 0; i < headerBytes; i++)
      {
        stream.read();
      }

      // get length of encoded string
      int strlen = 0;
      // write into array list
      for (int i = 0; i < sizeBitsLength; i++)
      {
        // read next byte of file
        int getlsb = stream.read();
        // %2 will give 0 or 1 for LSB
        getlsb = getlsb % 2;

        length.add(getlsb);
      }

      // reverse array list
      Collections.reverse(length);
      // convert to int
      for (Integer p: length)
      {
        strlen = strlen << 1;
        strlen += p;
      }

      // array to hold chars of the string
      char[] charArray = new char[strlen];

      // extract the message
      for (int i = 0; i < strlen; i++)
      {
        // store all LSBs from where message is stored
        ArrayList<Integer> charbits = new ArrayList<Integer>();

        // get att LSBs for each char
        for (int j = 0; j < byteLength; j++)
        {
          // read the next byte
          int getlsb = stream.read();
          // %2 will give 0 or 1 for LSB
          getlsb = getlsb % 2;
          // add to array
          charbits.add(getlsb);
        }
        // reverse this to get in correct order
        Collections.reverse(charbits);
        int letter = 0;
        // construct byte for string
        for (int p : charbits)
        {
          letter = letter << 1;
          letter += p;
        }
        // add to array of chars
        charArray[i] = (char) letter;
      }

      String finalMessage = "";

      // add chars to string
      for (int i = 0; i < strlen; i++)
      {
        finalMessage = finalMessage + charArray[i];
      }

      return finalMessage;
    }

    catch (FileNotFoundException e)
    {
      //error message as the user has selected a file which cannot be located
      System.err.println("The file " + stego_image + " cannot be found, please" +
          " try again");
      return "Fail";
    }
    catch (IOException e)
    {
      // TODO change this
      // error for stream.available()
      System.err.println("Error");
      return "Fail";
    }

  }

  /**
  The hideFile method hides any file (so long as there's enough capacity in the image file) in a cover image

  @param file_payload - the name of the file to be hidden, you can assume it is in the same directory as the program
  @param cover_image - the name of the cover image file, you can assume it is in the same directory as the program
  @return String - either 'Fail' to indicate an error in the hiding process, or the name of the stego image written out as a
  result of the successful hiding process
  */
  public String hideFile(String file_payload, String cover_image)
  {

    String outFile = "stegFile.bmp";

    FileReader reader = new FileReader(file_payload);

    try {
      FileOutputStream outStream = new FileOutputStream(outFile);
      FileInputStream inStream = new FileInputStream(cover_image);

      // used to test if image file is big enough to store
      File imageFile = new File(cover_image);
      File payloadFile = new File(file_payload);
      // if image file is not large enough to store file, return with error
      if (imageFile.length() < payloadFile.length()) {
        return "Error: file to store is too large for image file";
      }

      // write header bytes to file
      for (int i = 0; i < headerBytes; i++)
      {
        outStream.write(inStream.read());
      }

      // write file size to file
      for (int i = 0; i < fSizeBits; i++)
      {
        if (reader.hasNextBit())
        {
          // bit to store in lsb
          // if bit % 2 != 0 then the bit to pass is 1 else is 0
          // int bitToPass = reader.getNextBit % 2;
          int bitToPass = reader.getNextBit();
          // get next byte of image file
          int byteToPass = inStream.read();
          // swap lsb
          int bytesToWrite = swapLsb(bitToPass, byteToPass);
          // write to new file
          outStream.write(bytesToWrite);
        }
      }

      // write extension to file
      for (int i = 0; i < extBits; i++)
      {
        if (reader.hasNextBit())
        {
          int bitToPass = reader.getNextBit();
          // get next byte of image file
          int byteToPass = inStream.read();
          // swap lsb
          int bytesToWrite = swapLsb(bitToPass, byteToPass);
          // write to new file
          outStream.write(bytesToWrite);
        }
      }

      // write the rest of the original file
      while (reader.hasNextBit())
      {
        int bitToPass = reader.getNextBit();
        // get next byte of image file
        int byteToPass = inStream.read();
        // swap lsb
        int bytesToWrite = swapLsb(bitToPass, byteToPass);

        outStream.write(bytesToWrite);
      }

      // write rest of original image file
      int toWrite = 0;
      while ((toWrite = inStream.read()) != -1)
      {
        outStream.write(toWrite);
      }

    }
    catch (IOException e)
    {
      //error message as the user has selected a file which cannot be located
      System.err.println("The file " + file_payload + " cannot be found, please" +
          " try again");
      return "Fail";
    }

    return outFile;
  }

  /**
  The extractFile method hides any file (so long as there's enough capacity in the image file) in a cover image

  @param stego_image - the name of the file to be hidden, you can assume it is in the same directory as the program
  @return String - either 'Fail' to indicate an error in the extraction process, or the name of the file written out as a
  result of the successful extraction process
  */

  public String extractFile(String stego_image)
  {

    // to store the file name where the extracted file will be stored
    // extension is appended after extraction
    String extFile = "extractedFile";

    File f;
    FileInputStream stream = null;
    FileOutputStream outStream = null;

    try
    {
      // initialise input stream
      stream = new FileInputStream(stego_image);

      // skip header
      for (int i = 0; i < headerBytes; i++)
      {
        stream.read();
      }

      // get size of encoded file
      int fileSize = 0;
      ArrayList<Integer> size = new ArrayList<Integer>();
      // write into array list
      for (int i = 0; i < fSizeBits; i++)
      {
        // store next byte of file
        int getlsb = stream.read();
        // %2 will give 0 or 1 for LSB
        getlsb = getlsb % 2;
        size.add(getlsb);
      }
      // reverse array list
      Collections.reverse(size);
      // convert to int
      for (Integer p: size)
      {
        fileSize = fileSize << 1;
        fileSize += p;
      }

      // extract the file extension
      char[] charArray = new char[extBits];
      for (int i = 0; i < byteLength; i++)
      {
        // Store the extracted extension
        ArrayList<Integer> extension = new ArrayList<Integer>();

        // extract the LSBs for each byte
        for (int j = 0; j < byteLength; j++)
        {
          // read byte
          int getlsb = stream.read();
          // %2 will give 0 or 1 for LSB
          getlsb = getlsb % 2;
          // add this to array list
          extension.add(getlsb);
        }

        // reverse array list
        Collections.reverse(extension);
        int letter = 0;
        // construct byte for each char
        for (int p : extension)
        {
          letter = letter << 1;
          letter += p;
        }
        // convert to char and store in charArray
        charArray[i] = (char) letter;
      }

      String extensionStr = "";

      // add chars to string
      for (int i = 0; i < extBits; i++)
      {
        // only add characters to the string
        if (charArray[i] != 0)
        {
          extensionStr = extensionStr + charArray[i];
        }
      }

      // append extension to the file name
      extFile = extFile + extensionStr;
      // create file using the file name and extracted extension
      f = new File(extFile);
      f.createNewFile();

      outStream = new FileOutputStream(extFile);

      // store LSBs of each byte extracted
      int[] bytearr = new int[byteLength];
      // int written to output file made up of extracted LSBs
      int outputByte = 0;
      // used to cycle through bytearr
      int count = 0;

      // extract the rest fo the file
      for (int i = 0; i < fileSize; i++)
      {
        // get the LSB from each byte
        int getlsb = stream.read();
        // %2 to get if LSB is 0 or 1
        getlsb = getlsb % 2;

        // add LSBs of the byte of the original file to an int array
        bytearr[count] = getlsb;

        // when bytearr stores a bytes worth of information, store this in a integer
        if (count == 7) {
          for (int j = 7; j > -1; j--) {
            // shift left then add the LSB
            outputByte = outputByte << 1;
            outputByte += bytearr[j];
          }
          // write to output file
          outStream.write(outputByte);
          // reset byte to 0
          outputByte = 0;
        }
        // increase count
        count++;
        // count %8 to constantly fill bytearr
        count = count % 8;
      }
      // close input and output streams
      stream.close();
      outStream.close();
      // return name of extracted file
      return extFile;
    }
    catch (IOException e)
    {
      System.err.println("Error");
      return "Fail";
    }
}

  /**
   * This method swaps the least significant bit of a byte to match the bit passed in
   * @param bitToHide - the bit which is to replace the lsb of the byte of the image
   * @param byt - the current byte
   * @return the altered byte
   */
  public int swapLsb(int bitToHide,int byt)
  {
    // test if byte is even or odd
    // byt %2 - if 1, byt must end in 1, if 0 must end in 0

    // if bit to hide is 1 and LSB is 0, then shift LSB up by adding 1 to byt
    if (bitToHide > byt % 2)
    {
      byt += 1;
      return byt;
    }
    // if bit to hide is 0 and LSB is 1, then shift LSB down by subtracting 1 to byt
    if (bitToHide < byt % 2)
    {
      byt -= 1;
      return byt;
    }
    // else LSB is same as the bit to hide so do nothing to byt
    return byt;

  }

}
