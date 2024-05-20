<template>
  <v-app>
    <v-main>
      <h1>Crop Diversity Disk Usage</h1>
      <p>Each table shows the usage of the relevant folder on the Crop Diversity cluster, usage can be displayed in terms of total file size, number of directories and number of files. All results are sorted based on the total file size for that folder, in decending order.</p>

      <p>The System row shows the total usage for all users. The change column displays the change between the last two records.</p>

      <p><b>Please Note: We request that no user has a home folder larger than 10GB.</b> If your home folder is larger than this, consider if the scratch or projects folders would be more appropriate.</p>
      <MainCard v-bind:home="this.home" v-bind:projects="this.projects" v-bind:scratch="this.scratch"/>
    </v-main>
  </v-app>
</template>

<script>
import MainCard from './views/MainCard.vue'

export default {
  name: 'diskUsage',
  data: function() {
    return {
      home:{},
      projects: {},
      scratch: {},
      
      viewMenu: 'size'
    }
  },

  components: {
    MainCard
  },

  methods: {
    getData(){
      fetch('home.json')
      .then(response => {
        if(response.status !== 200) {
          console.log('Looks like there was a problem getting Home folder info. Status Code: ' + response.status);
          return;
        }
        // Examine the text in the response
        response.json().then(data => {
          this.home = data
        });
      })

      fetch('projects.json')
      .then(response => {
        if(response.status !== 200) {
          console.log('Looks like there was a problem getting Project folder info. Status Code: ' + response.status);
          return;
        }
        response.json().then(data => {
          this.projects = data
        });
      })

      fetch('scratch.json')
      .then(response => {
        if(response.status !== 200) {
          console.log('Looks like there was a problem getting Scratch folder info. Status Code: ' + response.status);
          return;
        }
        // Examine the text in the response
        response.json().then(data => {
          this.scratch = data
        });
      })
    }
  },
  beforeMount() {
    this.getData();
  }
}
 
</script>

<style>

body{
  width: 80%;
  margin:auto;
  color: #2c3e50;
  font-size: .85rem;
}

h1 {
  text-align: center;
  margin: 5px;
  font-weight: normal;
}

button {
  height: 30px;
}

p {
  margin-bottom: .85rem;
}

v-tabs {
  height: 30px;
}

table {
  width: 100%;
  margin: auto;
  table-layout: auto;  
  background-color:#fcf7f5;
  border-collapse:collapse;
}

.v-table > .v-table__wrapper > table > thead > tr > th, .v-table > .v-table__wrapper > table > tbody > tr > td {
  padding: 0 2px;
}

td, th, tr {  
  border: 1px solid #2c3e50;
}

.v-table .v-table__wrapper > table > tbody > tr:not(:last-child) > td, .v-table .v-table__wrapper > table > thead > tr > th {
  border-bottom: none;
}
</style>
