#!/usr/bin/env python
import xlrd
import argparse

# Used for generating term IDs.
COUNT = 0

def main(args):
    """
    Gets command line arguments, and starts Excel processing.

    :param args: the command line arguments
    """
    inputfile = args.inputfile
    # If user does not provide output file name, use a default one.
    outputfile = "CarePathwaysQuestions.tsv" if not args.outputfile else args.outputfile
    # Process the file data, and save the result.
    process_data(inputfile, outputfile)


def process_data(inputfile, outputfile):
    """
    Open the Excel file, read the data, and convert to a TSV format.

    :param inputfile:  the name of the Excel file that contains the data
    :param outputfile: the name of the TSV file to which processed data will be written
    """
    # Open Excel file.
    book = xlrd.open_workbook(inputfile)

    # The question sheet.
    question_sheet = book.sheet_by_index(0)
    # Process question sheet data.
    format_sheet(question_sheet, 1, question_sheet.nrows, outputfile)

def format_sheet(sheet, start_pos, num_rows, file_name):
    """
    Read the sheet data and write it in correct format to the output file.

    :param sheet:     the Excel sheet that is being processed
    :param start_pos: the line number from which to start reading data
    :param num_rows:  the total number of data rows
    :param file_name: the name of the output file
    """
    f = open(file_name, 'a')
    post_test = "post test"
    ignore_list = ["routine care", "other (specify):"]

    category = "pre test"
    # Process data one row at a time.
    for i in range(start_pos, num_rows):
        row = sheet.row_values(i)
        # Look at the first column.
        raw_first = row[0].strip()
        if post_test.upper() in raw_first:
            category = post_test
            continue

        if not raw_first or ("MODULE" in raw_first) or any(item == raw_first for item in ignore_list):
            continue

        data = category + '\t' + _attach_id(raw_first)
        f.write(data + '\n')
    # Close output file.
    f.close()


def _attach_id(item):
    """
    Generates and attaches an identifier to the item. Stores this identifier iff store_item is set to true.

    :param item:       the item that needs an identifier attached to it
    :return:           the item with the attached identifier
    """
    global COUNT

    str_count = str(COUNT)
    COUNT += 1
    return item + "\tCPQ:" + str_count


if __name__ == "__main__":
    # Allow user to provide input and output file names.
    parser = argparse.ArgumentParser()
    parser.add_argument("-i", "--inputfile", help="Excel input file")
    parser.add_argument("-o", "--outputfile", help="Output file")
    main(parser.parse_args())
