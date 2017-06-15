# Introduction
Currently we are facing more and more problems with balancing network. Particurarly renewal energy sources does not provide stable output however with great probability we can predict flutuactions in near future. This project is and proof of concept to blance grid by precisely sterring heaters in distributed network of houses.
Assuming the heaters are switching on and off to keep desired temperature we can sligthtly tune the timing and provide the peak to network by decreasing or increasing load.

# Experimental setup
Our testing setup is based on house termal simulator developed in DTU (it assumes the temperature can be set independly in all rooms and takes also into consideration state of doors and windows). Houses are connected into robust distributed network with aggregation nodes.
You are welcome to read more about in **Summary.pdf**

# Run
The project is meant to be run by executing the script run_terminology.sh however to run commands in split view it depends on external terminal emulator - terminology.
https://www.enlightenment.org/about-terminology

It can be also run in seperate windows (12 of them) by executing script run.sh.

If more controll is needed or extra paramets used each part of the network can be executed separately by running grid.sh, aggregator.sh, house_controller.sh accordingly.
