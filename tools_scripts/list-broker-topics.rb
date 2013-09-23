#!/usr/bin/env ruby
 
require 'rubygems'
require 'mqtt'
 
MQTT::Client.connect('localhost',1883) do |client|
        client.get('#') do |topic,message|
                puts "#{topic}: #{message}"
        end
end
