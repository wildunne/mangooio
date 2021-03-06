<#include "header.ftl">
<section class="content-header">
	<h1>Dashboard</h1>
</section>
<section class="content">
<div class="row">
	<div class="col-lg-12">
	    <div class="small-box bg-green">
    	    <div class="inner">
              <h3>${uptime}</h3>
              <p>Application started</p>
            </div>
        </div>
    </div>
</div>
<div class="row">
	<div class="col-xs-12">
		<div class="box">
	    	<div class="box-header">
				<div class="form-group">
	            	<input type="text" name="table_search" id="filter" class="form-control" placeholder="Start typing what you are looking for...">
	            </div>
	        </div>
	        <div class="box-body table-responsive no-padding">
	        	<table class="table table-hover">
	            	<thead>
						<tr>
							<th data-sort="string"><b>Property</b></th>
							<th data-sort="string"><b>Value</b></th>
						</tr>
					</thead>
					<tbody class="searchable">
					<#list properties as key, value>
						<tr>
							<td>${key}</td>
							<td>${value}</td>
						</tr>
					</#list>
	                </tbody>
	            </table>
			</div>
		</div>
	</div>
</div>
</section>
<#include "footer.ftl">