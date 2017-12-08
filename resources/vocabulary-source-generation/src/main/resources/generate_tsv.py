#!/usr/bin/env python
import xlrd
import argparse

# Used for generating term IDs. Numbers 1 and 2 are taken by the "Test" and "Care" terms, respectively.
COUNT = 3
TESTS_MAP = {}


def main(args):
    """
    Gets command line arguments, and starts Excel processing.

    :param args: the command line arguments
    """
    inputfile = args.ifile
    # If user does not provide output file name, use a default one.
    outputfile = "CarePathways.tsv" if not args.ofile else args.ofile
    # Process the file data, and save the result.
    process_data(inputfile, outputfile)


def process_data(inputfile, outputfile):
    """
    Open the Excel file, read the data, and convert to a TSV format.

    :param inputfile:  the name of the Excel file that contains the data
    :param outputfile: the name of the TSV file to which processed data will be written
    """
    global TESTS_MAP

    # Open Excel file.
    book = xlrd.open_workbook(inputfile)
    # Get the total number of sheets.
    sheet_number = book.nsheets

    # The Module A sheet.
    mod_a_sheet = book.sheet_by_index(1)
    # Process Module A data.
    format_sheet(mod_a_sheet, 2, mod_a_sheet.nrows, outputfile, "Test [CP:1]")

    # Clear the map.
    TESTS_MAP = {}
    # The Module B sheet.
    mod_b_sheet = book.sheet_by_index(2)
    # Process Module B data.
    format_sheet(mod_b_sheet, 3, mod_b_sheet.nrows, outputfile, "Care [CP:2]")


def format_sheet(sheet, start_pos, num_rows, file_name, root):
    """
    Read the sheet data and write it in correct format to the output file.

    :param sheet: the Excel sheet that is being processed
    :param start_pos: the line number from which to start reading data
    :param num_rows: the total number of data rows
    :param file_name: the name of the output file
    :param root: the root term
    """
    f = open(file_name, 'a')
    data = "ERROR"

    # Write the root node on its own line.
    f.write(root + '\n')
    # Process data one row at a time.
    for i in range(start_pos, num_rows):
        row = sheet.row_values(i)
        # Look at the first column.
        raw_first = row[0].strip();
        # Ignore junk data.
        if raw_first.lower() != "other" and raw_first.lower() != "other:" and raw_first != "date data entered":
            # If we're not looking at an empty string, then we have a data term.
            if raw_first != "":
                # Accumulated data string. Attach a term ID to the raw_first string.
                data = root + '\t' + _attach_id(raw_first, True)
                # Write the path to the first column term on its own line.
                f.write(data + '\n')
            # Get the second and third columns.
            second_cell = _split_items(row[1].strip())
            third_cell = _split_items(row[2].strip())
            # Accumulate that data as well.
            _write_line(f, data, second_cell, third_cell)
    # Close output file.
    f.close()


def _split_items(cell_str):
    """
    Some cells contain data for multiple terms. This data is separated by commas. Split the terms, and return that list.

    :param cell_str: the string stored in the Excel data cell
    :return: the list of terms
    """
    item_list = [x.strip() for x in cell_str.split(',')]
    return [] if (len(item_list) == 0 or item_list[0] == '') else item_list


def _write_line(f, data, second, third):
    """
    Processes and writes the data written in accumulated string, second and third cells.

    :param f:      the output file
    :param data:   the accumulated data string
    :param second: data stored in the second column of the Excel sheet
    :param third:  data stored in the third column of the Excel sheet
    """
    second_len = len(second)
    third_len = len(third)

    # If we stil have something in the second cell, process that data.
    if second_len > 0:
        # Look at all the terms in the second cell.
        for item in second:
            # Discard junk.
            if item.lower() != "other" and item.lower() != "other:":
                # If the second cell only has one item, write the path to that item on its own line.
                if second_len == 1 and third_len > 0:
                    with_id = _attach_id(item, True)
                    f.write(data + '\t' + with_id + '\n')
                # Accumulate the item in second cell into data, and recurse.
                with_id = _attach_id(item, True) if (second_len == 1 and third_len == 0) else _attach_id(item, False)
                line = data + "\t" + with_id
                _write_line(f, line, third, [])
    # Otherwise we've looked at everything, write the accumulated data string.
    else:
        f.write(data + '\n')


def _attach_id(item, store_item):
    """
    Generates and attaches an identifier to the item. Stores this identifier iff store_item is set to true.

    :param item:       the item that needs an identifier attached to it
    :param store_item: true iff the identifier and item pair should be stored for future retrieval
    :return:           the item with the attached identifier
    """
    global TESTS_MAP
    global COUNT

    # If the ID was assigned to this item already, get it.
    if item in TESTS_MAP:
        test_id = TESTS_MAP.get(item)
    # Otherwise, update count, generate ID, and if this ID should be stored, store it in map.
    else:
        old_count = COUNT
        COUNT += 1
        test_id = "CP:" + str(old_count)
        if store_item:
            TESTS_MAP[item] = test_id
    # Return the ID.
    return item + " [" + test_id + "]"


if __name__ == "__main__":
    # Allow user to provide input and output file names.
    parser = argparse.ArgumentParser()
    parser.add_argument("-i", "--inputfile", help="Excel input file")
    parser.add_argument("-o", "--outputfile", help="Output file")
    args = parser.parse_args()
    main(args)

# cat CarePathways.tsv | awk --field-separator="\t" '{ print NF, $0 }' | sort -n -s | cut -d" " -f2- > CarePathwaysOrdered.tsv
